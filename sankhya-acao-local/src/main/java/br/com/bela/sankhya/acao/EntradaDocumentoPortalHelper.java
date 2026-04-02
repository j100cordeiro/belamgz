package br.com.bela.sankhya.acao;

import br.com.sankhya.jape.EntityFacade;
import br.com.sankhya.jape.dao.EntityDAO;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.vo.PrePersistEntityState;
import br.com.sankhya.jape.wrapper.JapeFactory;
import br.com.sankhya.jape.wrapper.JapeWrapper;
import br.com.sankhya.jape.wrapper.fluid.FluidCreateVO;
import br.com.sankhya.modelcore.auth.AuthenticationInfo;
import br.com.sankhya.modelcore.util.EntityFacadeFactory;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

final class EntradaDocumentoPortalHelper {

    private final Map<String, Object> valoresCamposCabecalho = new HashMap<String, Object>();
    private final List<Map<String, Object>> dadosItens = new ArrayList<Map<String, Object>>();
    private Map<String, Object> valoresCamposItem = new HashMap<String, Object>();
    private final EntityFacade entityFacade = EntityFacadeFactory.getDWFFacade();
    private final JapeWrapper cabecalhoNotaDao = JapeFactory.dao("CabecalhoNota");
    private final JapeWrapper produtoDao = JapeFactory.dao("Produto");

    private DynamicVO cabecalhoNotaVO;
    private DynamicVO modeloCabecalhoVO;

    void setModeloCabecalho(DynamicVO modeloCabecalhoVO) {
        this.modeloCabecalhoVO = modeloCabecalhoVO;
    }

    void setValorCampoCabecalho(String campo, Object valor) {
        valoresCamposCabecalho.put(campo, valor);
    }

    void setValorCampoItem(String campo, Object valor) {
        valoresCamposItem.put(campo, valor);
    }

    void salvarItem() {
        dadosItens.add(new HashMap<String, Object>(valoresCamposItem));
        valoresCamposItem = new HashMap<String, Object>();
    }

    BigDecimal getNumeroUnicoNota() {
        return cabecalhoNotaVO.asBigDecimal("NUNOTA");
    }

    void processar() throws Exception {
        criarCabecalho();
        criarItens();
    }

    private void criarCabecalho() throws Exception {
        if (modeloCabecalhoVO != null) {
            cabecalhoNotaVO = EntradaDynamicVOHelper.duplicarCabecalhoNota(entityFacade, modeloCabecalhoVO,
                    valoresCamposCabecalho);
            return;
        }

        FluidCreateVO cabecalhoNotaFCVO = cabecalhoNotaDao.create();

        if (!valoresCamposCabecalho.containsKey("NUNOTA")) {
            cabecalhoNotaFCVO.set("NUNOTA", null);
        }
        if (!valoresCamposCabecalho.containsKey("NUMNOTA")) {
            cabecalhoNotaFCVO.set("NUMNOTA", BigDecimal.ZERO);
        }
        if (!valoresCamposCabecalho.containsKey("DTNEG")) {
            cabecalhoNotaFCVO.set("DTNEG", new Timestamp(System.currentTimeMillis()));
        }
        if (!valoresCamposCabecalho.containsKey("DTENTSAI")) {
            cabecalhoNotaFCVO.set("DTENTSAI", new Timestamp(System.currentTimeMillis()));
        }
        if (!valoresCamposCabecalho.containsKey("DTMOV")) {
            cabecalhoNotaFCVO.set("DTMOV", new Timestamp(System.currentTimeMillis()));
        }
        if (!valoresCamposCabecalho.containsKey("CIF_FOB")) {
            cabecalhoNotaFCVO.set("CIF_FOB", "C");
        }

        for (Map.Entry<String, Object> entry : valoresCamposCabecalho.entrySet()) {
            cabecalhoNotaFCVO.set(entry.getKey(), entry.getValue());
        }

        cabecalhoNotaVO = cabecalhoNotaFCVO.save();
    }

    private void criarItens() throws Exception {
        Collection<PrePersistEntityState> itensNotaPPES = new ArrayList<PrePersistEntityState>();
        int sequencia = 1;

        for (Map<String, Object> valoresItem : dadosItens) {
            EntityDAO dao = entityFacade.getDAOInstance("ItemNota");
            DynamicVO itemVO = (DynamicVO) dao.getDefaultValueObjectInstance();
            itemVO.setAceptTransientProperties(true);
            BigDecimal codigoProduto = (BigDecimal) valoresItem.get("CODPROD");
            DynamicVO produtoVO = produtoDao.findByPK(codigoProduto);

            if (!valoresItem.containsKey("CODVOL")) {
                valoresItem.put("CODVOL", produtoVO.asString("CODVOL"));
            }
            if (!valoresItem.containsKey("CODLOCALORIG")) {
                valoresItem.put("CODLOCALORIG", produtoVO.asBigDecimal("CODLOCALPADRAO"));
            }
            if (!valoresItem.containsKey("SEQUENCIA")) {
                valoresItem.put("SEQUENCIA", new BigDecimal(sequencia++));
            }
            if (!valoresItem.containsKey("VLRTOT")
                    && valoresItem.containsKey("VLRUNIT")
                    && valoresItem.containsKey("QTDNEG")) {
                BigDecimal vlrUnit = (BigDecimal) valoresItem.get("VLRUNIT");
                BigDecimal qtdNeg = (BigDecimal) valoresItem.get("QTDNEG");
                valoresItem.put("VLRTOT", vlrUnit.multiply(qtdNeg));
            }

            for (Map.Entry<String, Object> entry : valoresItem.entrySet()) {
                itemVO.setProperty(entry.getKey(), entry.getValue());
            }

            PrePersistEntityState itemPreState = PrePersistEntityState.build(entityFacade, "ItemNota", itemVO, null);
            itensNotaPPES.add(itemPreState);
        }

        AuthenticationInfo auth = AuthenticationInfo.getCurrent();
        if (auth == null) {
            throw new IllegalStateException("Nao foi possivel identificar o usuario autenticado para criar os itens.");
        }

        Object cacHelper = Class.forName("br.com.sankhya.modelcore.comercial.centrais.CACHelper")
                .getDeclaredConstructor()
                .newInstance();

        cacHelper.getClass()
                .getMethod("incluirAlterarItem", BigDecimal.class, AuthenticationInfo.class, Collection.class,
                        boolean.class)
                .invoke(cacHelper, cabecalhoNotaVO.asBigDecimal("NUNOTA"), auth, itensNotaPPES, true);
    }
}
