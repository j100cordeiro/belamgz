package br.com.bela.sankhya.evento;

import java.lang.reflect.Method;
import java.sql.Timestamp;
import java.util.Date;
import java.util.logging.Logger;

import br.com.sankhya.extensions.eventoprogramavel.EventoProgramavelJava;
import br.com.sankhya.jape.event.PersistenceEvent;
import br.com.sankhya.jape.vo.DynamicVO;

/**
 * Mantem DTENTSAI igual a DTNEG em movimentos de compra da TGFCAB.
 *
 * Regras:
 * - no before insert/update: sincroniza o VO em memoria;
 * - no before update: quando o runtime expuser oldVO, bloqueia edicao manual de DTENTSAI;
 * - no after insert/update: nao faz round-trip ao banco; a persistencia ocorre pelo proprio before.
 */
public class SincronizaDtEntSaiCompraTGFCAB extends AbstractEventoProgramavel implements EventoProgramavelJava {

    private static final Logger LOGGER = Logger.getLogger(SincronizaDtEntSaiCompraTGFCAB.class.getName());

    private static final String FIELD_NUNOTA = "NUNOTA";
    private static final String FIELD_TIPMOV = "TIPMOV";
    private static final String FIELD_DTNEG = "DTNEG";
    private static final String FIELD_DTENTSAI = "DTENTSAI";
    private static final String TIPMOV_COMPRA = "C";
    private static final String TIPMOV_PEDIDO_COMPRA = "O";

    @Override
    public void beforeInsert(PersistenceEvent event) throws Exception {
        sincronizarDtEntSai(extrairVo(event));
    }

    @Override
    public void beforeUpdate(PersistenceEvent event) throws Exception {
        DynamicVO vo = extrairVo(event);
        DynamicVO oldVo = extrairOldVoCompat(event);
        validarEdicaoManualDtEntSai(vo, oldVo);
        sincronizarDtEntSai(vo);
    }

    @Override
    public void afterInsert(PersistenceEvent event) throws Exception {
        // Sem round-trip ao banco. O VO ja foi sincronizado no beforeInsert.
    }

    @Override
    public void afterUpdate(PersistenceEvent event) throws Exception {
        // Sem round-trip ao banco. O VO ja foi sincronizado no beforeUpdate.
    }

    private void sincronizarDtEntSai(DynamicVO vo) throws Exception {
        if (vo == null) {
            return;
        }

        if (!isMovimentoCompra(getString(vo, FIELD_TIPMOV))) {
            return;
        }

        Object dtneg = getPropertyValue(vo, FIELD_DTNEG);
        if (dtneg == null) {
            LOGGER.fine("[DTENTSAI] DTNEG nula em movimento de compra. Sincronizacao ignorada.");
            return;
        }

        setPropertyValue(vo, FIELD_DTENTSAI, normalizarData(dtneg));
    }

    private void validarEdicaoManualDtEntSai(DynamicVO vo, DynamicVO oldVo) throws Exception {
        if (vo == null) {
            return;
        }

        if (!isMovimentoCompra(getString(vo, FIELD_TIPMOV))) {
            return;
        }

        if (oldVo == null || !isMovimentoCompra(getString(oldVo, FIELD_TIPMOV))) {
            return;
        }

        Object dtnegAtual = getPropertyValue(vo, FIELD_DTNEG);
        Object dtEntSaiAtual = getPropertyValue(vo, FIELD_DTENTSAI);
        Object dtnegAnterior = getPropertyValue(oldVo, FIELD_DTNEG);
        Object dtEntSaiAnterior = getPropertyValue(oldVo, FIELD_DTENTSAI);

        boolean dtnegMudou = !datasIguais(dtnegAtual, dtnegAnterior);
        boolean dtEntSaiMudou = !datasIguais(dtEntSaiAtual, dtEntSaiAnterior);

        if (dtEntSaiMudou && !dtnegMudou) {
            throw new Exception("Nao e permitido editar manualmente a Dt. Entrada/Saida. "
                    + "Altere apenas a Dt. Negociacao.");
        }
    }

    private DynamicVO extrairVo(PersistenceEvent event) {
        return event == null ? null : event.getVo();
    }

    private DynamicVO extrairOldVoCompat(PersistenceEvent event) {
        if (event == null) {
            return null;
        }
        try {
            Method method = event.getClass().getMethod("getOldVO");
            Object value = method.invoke(event);
            return value instanceof DynamicVO ? (DynamicVO) value : null;
        } catch (Exception ignored) {
            try {
                Method method = event.getClass().getMethod("getOldVo");
                Object value = method.invoke(event);
                return value instanceof DynamicVO ? (DynamicVO) value : null;
            } catch (Exception ignoredAgain) {
                return null;
            }
        }
    }

    private Object getPropertyValue(DynamicVO vo, String field) {
        if (vo == null || field == null) {
            return null;
        }
        return vo.getProperty(field);
    }

    private void setPropertyValue(DynamicVO vo, String field, Object value) throws Exception {
        try {
            Method setter = vo.getClass().getMethod("setProperty", String.class, Object.class);
            setter.invoke(vo, field, value);
            return;
        } catch (NoSuchMethodException ignored) {
            // fallback abaixo
        }

        try {
            Method setter = vo.getClass().getMethod("set", String.class, Object.class);
            setter.invoke(vo, field, value);
            return;
        } catch (NoSuchMethodException ignored) {
            // fallback abaixo
        }

        throw new Exception("Nao foi possivel atribuir " + field + " no VO da TGFCAB.");
    }

    private Object normalizarData(Object value) {
        if (value instanceof Timestamp) {
            return value;
        }
        if (value instanceof Date) {
            return new Timestamp(((Date) value).getTime());
        }
        return value;
    }

    private String getString(DynamicVO vo, String field) {
        Object value = getPropertyValue(vo, field);
        return value == null ? null : value.toString();
    }

    private String trimToEmpty(String value) {
        return value == null ? "" : value.trim();
    }

    private boolean isMovimentoCompra(String tipmov) {
        String valor = trimToEmpty(tipmov);
        return TIPMOV_COMPRA.equalsIgnoreCase(valor)
                || TIPMOV_PEDIDO_COMPRA.equalsIgnoreCase(valor);
    }

    private boolean datasIguais(Object data1, Object data2) {
        Timestamp ts1 = toTimestamp(data1);
        Timestamp ts2 = toTimestamp(data2);

        if (ts1 == null && ts2 == null) {
            return true;
        }
        if (ts1 == null || ts2 == null) {
            return false;
        }
        return ts1.getTime() == ts2.getTime();
    }

    private Timestamp toTimestamp(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Timestamp) {
            return (Timestamp) value;
        }
        if (value instanceof Date) {
            return new Timestamp(((Date) value).getTime());
        }
        return null;
    }

}
