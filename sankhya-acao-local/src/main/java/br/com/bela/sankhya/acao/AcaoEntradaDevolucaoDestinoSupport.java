package br.com.bela.sankhya.acao;

import br.com.sankhya.extensions.actionbutton.ContextoAcao;
import br.com.sankhya.extensions.actionbutton.Registro;
import br.com.sankhya.jape.EntityFacade;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.wrapper.JapeFactory;
import br.com.sankhya.jape.wrapper.JapeWrapper;
import br.com.sankhya.jape.wrapper.fluid.FluidUpdateVO;
import br.com.sankhya.modelcore.util.EntityFacadeFactory;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class AcaoEntradaDevolucaoDestinoSupport {

    private static final BigDecimal LOCAL_TRIAGEM = new BigDecimal("30100");
    private static final BigDecimal LOCAL_10100 = new BigDecimal("10100");
    private static final BigDecimal LOCAL_20100 = new BigDecimal("20100");
    private static final BigDecimal TOP_TRANSFERENCIA = new BigDecimal("2152");
    private static final BigDecimal PARCEIRO_INTERNO_PADRAO = new BigDecimal("582611");
    private static final BigDecimal CODNAT_PADRAO = new BigDecimal("10010201");
    private static final BigDecimal CODTIPVENDA_PADRAO = BigDecimal.ZERO;
    private static final BigDecimal CODCENCUS_PADRAO = BigDecimal.ZERO;
    private static final BigDecimal CODCFO_DESTINO_PADRAO = new BigDecimal("6202");
    private static final String SERIE_PADRAO = "1";
    private static final Set<BigDecimal> DESTINOS_PERMITIDOS = Collections.unmodifiableSet(
            new LinkedHashSet<BigDecimal>(Arrays.asList(LOCAL_10100, LOCAL_20100)));

    private AcaoEntradaDevolucaoDestinoSupport() {
    }

    static void executar(ContextoAcao contexto, BigDecimal localDestino) throws Exception {
        Registro[] linhas = contexto.getLinhas();
        if (linhas == null || linhas.length == 0) {
            contexto.mostraErro("Selecione ao menos uma nota para gerar a entrada.");
            return;
        }

        validarDestino(localDestino, contexto);

        JapeWrapper cabecalhoDao = JapeFactory.dao("CabecalhoNota");
        JapeWrapper itemDao = JapeFactory.dao("ItemNota");
        List<BigDecimal> notasGeradas = new ArrayList<BigDecimal>();

        for (BigDecimal nunotaOrigem : coletarNotasSelecionadas(linhas, contexto)) {
            DynamicVO notaOrigem = cabecalhoDao.findOne("NUNOTA = ?", nunotaOrigem);
            if (notaOrigem == null) {
                contexto.mostraErro("Nao foi possivel localizar a nota " + nunotaOrigem.toPlainString() + ".");
                return;
            }

            validarNotaOrigem(notaOrigem, itemDao, cabecalhoDao, contexto);

            Collection<DynamicVO> itensOrigem = itemDao.find(
                    "NUNOTA = ? AND SEQUENCIA > 0 AND CODLOCALORIG = ?",
                    nunotaOrigem,
                    LOCAL_TRIAGEM);

            if (itensOrigem == null || itensOrigem.isEmpty()) {
                contexto.mostraErro("Nao encontrado nenhum item na Triagem (30100) para a nota "
                        + nunotaOrigem.toPlainString() + ".");
                return;
            }

            DynamicVO modeloTransferencia = buscarModeloTransferencia(cabecalhoDao, notaOrigem.asBigDecimal("CODEMP"));
            BigDecimal nunotaGerada = gerarNotaTransferencia(notaOrigem, itensOrigem, localDestino, modeloTransferencia);
            vincularNotaOrigem(cabecalhoDao, notaOrigem, nunotaGerada);
            notasGeradas.add(nunotaGerada);
        }

        contexto.setMensagemRetorno("Entrada gerada com destino " + localDestino.toPlainString()
                + ". Nota(s) gerada(s): " + juntarNotas(notasGeradas) + ".");
    }

    private static void validarDestino(BigDecimal localDestino, ContextoAcao contexto) throws Exception {
        if (localDestino == null || !DESTINOS_PERMITIDOS.contains(localDestino)) {
            contexto.mostraErro("Destino invalido. Use 10100 ou 20100.");
        }
    }

    private static Set<BigDecimal> coletarNotasSelecionadas(Registro[] linhas, ContextoAcao contexto) throws Exception {
        Set<BigDecimal> notas = new LinkedHashSet<BigDecimal>();

        for (Registro linha : linhas) {
            BigDecimal nunota = asBigDecimal(linha.getCampo("NUNOTA"));
            if (nunota == null) {
                contexto.mostraErro("Nao foi possivel identificar a NUNOTA de uma das linhas selecionadas.");
                return Collections.emptySet();
            }
            notas.add(nunota);
        }

        return notas;
    }

    private static void validarNotaOrigem(DynamicVO notaOrigem, JapeWrapper itemDao, JapeWrapper cabecalhoDao,
            ContextoAcao contexto) throws Exception {
        BigDecimal nunotaOrigem = notaOrigem.asBigDecimal("NUNOTA");

        if (!"D".equals(notaOrigem.asString("TIPMOV"))) {
            contexto.mostraErro("A nota " + nunotaOrigem.toPlainString()
                    + " nao e uma devolucao valida para esta acao.");
            return;
        }

        if (!"L".equals(notaOrigem.asString("STATUSNOTA"))) {
            contexto.mostraErro("A nota " + nunotaOrigem.toPlainString()
                    + " precisa estar confirmada para gerar a entrada.");
            return;
        }

        BigDecimal nunotaDev = notaOrigem.asBigDecimal("AD_NUNOTADEV");
        if (nunotaDev != null) {
            DynamicVO notaGerada = cabecalhoDao.findOne("NUNOTA = ?", nunotaDev);
            if (notaGerada != null && TOP_TRANSFERENCIA.compareTo(notaGerada.asBigDecimal("CODTIPOPER")) == 0) {
                contexto.mostraErro("A nota " + nunotaOrigem.toPlainString()
                        + " ja possui entrada gerada: " + nunotaDev.toPlainString() + ".");
                return;
            }

            FluidUpdateVO clearVO = cabecalhoDao.prepareToUpdate(notaOrigem);
            clearVO.set("AD_NUNOTADEV", null);
            clearVO.update();
        }

        Collection<DynamicVO> itensTriagem = itemDao.find(
                "NUNOTA = ? AND SEQUENCIA > 0 AND CODLOCALORIG = ?",
                nunotaOrigem,
                LOCAL_TRIAGEM);

        if (itensTriagem == null || itensTriagem.isEmpty()) {
            contexto.mostraErro("Nao encontrado nenhum item na Triagem (30100) para a nota "
                    + nunotaOrigem.toPlainString() + ".");
        }
    }

    private static DynamicVO buscarModeloTransferencia(JapeWrapper cabecalhoDao, BigDecimal codEmp) throws Exception {
        DynamicVO modelo = cabecalhoDao.findOne(
                "CODTIPOPER = ? AND CODEMP = ? AND TIPMOV = 'T'",
                TOP_TRANSFERENCIA,
                codEmp);

        if (modelo != null) {
            return modelo;
        }

        return cabecalhoDao.findOne("CODTIPOPER = ? AND TIPMOV = 'T'", TOP_TRANSFERENCIA);
    }

    private static BigDecimal gerarNotaTransferencia(DynamicVO notaOrigem,
            Collection<DynamicVO> itensOrigem,
            BigDecimal localDestino,
            DynamicVO modeloTransferencia) throws Exception {

        EntradaDocumentoPortalHelper criador = new EntradaDocumentoPortalHelper();
        criador.setModeloCabecalho(modeloTransferencia);

        Timestamp agora = new Timestamp(System.currentTimeMillis());
        BigDecimal codEmp = notaOrigem.asBigDecimal("CODEMP");
        BigDecimal codVend = notaOrigem.asBigDecimal("CODVEND");
        BigDecimal codParcInterno = modeloTransferencia != null
                ? modeloTransferencia.asBigDecimal("CODPARC")
                : PARCEIRO_INTERNO_PADRAO;
        BigDecimal codTipVenda = modeloTransferencia != null
                ? nvl(modeloTransferencia.asBigDecimal("CODTIPVENDA"), CODTIPVENDA_PADRAO)
                : CODTIPVENDA_PADRAO;
        BigDecimal codNat = modeloTransferencia != null
                ? nvl(modeloTransferencia.asBigDecimal("CODNAT"), CODNAT_PADRAO)
                : CODNAT_PADRAO;
        BigDecimal codCenCus = modeloTransferencia != null
                ? nvl(modeloTransferencia.asBigDecimal("CODCENCUS"), CODCENCUS_PADRAO)
                : CODCENCUS_PADRAO;
        String serie = modeloTransferencia != null
                ? nvl(modeloTransferencia.asString("SERIENOTA"), SERIE_PADRAO)
                : SERIE_PADRAO;

        criador.setValorCampoCabecalho("CODTIPOPER", TOP_TRANSFERENCIA);
        criador.setValorCampoCabecalho("TIPMOV", "T");
        criador.setValorCampoCabecalho("STATUSNOTA", "A");
        criador.setValorCampoCabecalho("PENDENTE", "N");
        criador.setValorCampoCabecalho("NUMNOTA", BigDecimal.ZERO);
        criador.setValorCampoCabecalho("CODEMP", codEmp);
        criador.setValorCampoCabecalho("CODEMPNEGOC", codEmp);
        criador.setValorCampoCabecalho("CODPARC", codParcInterno);
        criador.setValorCampoCabecalho("CODTIPVENDA", codTipVenda);
        criador.setValorCampoCabecalho("CODNAT", codNat);
        criador.setValorCampoCabecalho("CODCENCUS", codCenCus);
        criador.setValorCampoCabecalho("CODVEND", nvl(codVend, BigDecimal.ZERO));
        criador.setValorCampoCabecalho("SERIENOTA", serie);
        criador.setValorCampoCabecalho("DTNEG", agora);
        criador.setValorCampoCabecalho("DTMOV", agora);
        criador.setValorCampoCabecalho("DTENTSAI", agora);
        criador.setValorCampoCabecalho("DTFATUR", agora);
        criador.setValorCampoCabecalho("OBSERVACAO", montarObservacao(notaOrigem));

        for (DynamicVO itemOrigem : itensOrigem) {
            criador.setValorCampoItem("CODPROD", itemOrigem.asBigDecimal("CODPROD"));
            criador.setValorCampoItem("QTDNEG", itemOrigem.asBigDecimal("QTDNEG"));
            criador.setValorCampoItem("CODVOL", itemOrigem.asString("CODVOL"));
            criador.setValorCampoItem("CONTROLE", itemOrigem.asString("CONTROLE"));
            criador.setValorCampoItem("CODCFO", itemOrigem.asBigDecimal("CODCFO"));
            criador.setValorCampoItem("CODLOCALORIG", LOCAL_TRIAGEM);
            criador.setValorCampoItem("CODLOCALDEST", localDestino);
            criador.setValorCampoItem("VLRUNIT", itemOrigem.asBigDecimal("VLRUNIT"));
            criador.setValorCampoItem("VLRTOT", itemOrigem.asBigDecimal("VLRTOT"));
            criador.salvarItem();
        }

        criador.processar();

        BigDecimal nunotaGerada = criador.getNumeroUnicoNota();
        ajustarDestinoNotaGerada(nunotaGerada, localDestino);
        return nunotaGerada;
    }

    private static void ajustarDestinoNotaGerada(BigDecimal nunotaGerada, BigDecimal localDestino) throws Exception {
        JapeWrapper itemDao = JapeFactory.dao("ItemNota");
        EntityFacade entityFacade = EntityFacadeFactory.getDWFFacade();

        Collection<DynamicVO> itensPositivos = itemDao.find("NUNOTA = ? AND SEQUENCIA > 0", nunotaGerada);
        Collection<DynamicVO> itensNegativos = itemDao.find("NUNOTA = ? AND SEQUENCIA < 0", nunotaGerada);

        if (itensNegativos == null || itensNegativos.isEmpty()) {
            for (DynamicVO itemPositivo : itensPositivos) {
                Map<String, Object> overrides = new HashMap<String, Object>();
                overrides.put("NUNOTA", nunotaGerada);
                overrides.put("SEQUENCIA", itemPositivo.asBigDecimal("SEQUENCIA").negate());
                overrides.put("CODLOCALORIG", localDestino);
                overrides.put("CODUSU", null);
                overrides.put("CODCFO", gerarCodCfoDestino(itemPositivo.asBigDecimal("CODCFO")));

                EntradaDynamicVOHelper.duplicarItemNota(entityFacade, itemPositivo, overrides);
            }
            return;
        }

        for (DynamicVO itemNegativo : itensNegativos) {
            FluidUpdateVO updateVO = itemDao.prepareToUpdate(itemNegativo);
            updateVO.set("CODLOCALORIG", localDestino);
            updateVO.set("CODCFO", gerarCodCfoDestino(itemNegativo.asBigDecimal("CODCFO")));
            updateVO.update();
        }
    }

    private static BigDecimal gerarCodCfoDestino(BigDecimal codCfoAtual) {
        if (codCfoAtual == null) {
            return CODCFO_DESTINO_PADRAO;
        }

        if (codCfoAtual.compareTo(new BigDecimal("5000")) >= 0) {
            return codCfoAtual;
        }

        return codCfoAtual.add(new BigDecimal("4000"));
    }

    private static void vincularNotaOrigem(JapeWrapper cabecalhoDao, DynamicVO notaOrigem, BigDecimal nunotaGerada)
            throws Exception {
        FluidUpdateVO updateVO = cabecalhoDao.prepareToUpdate(notaOrigem);
        updateVO.set("AD_NUNOTADEV", nunotaGerada);
        updateVO.update();
    }

    private static String montarObservacao(DynamicVO notaOrigem) throws Exception {
        BigDecimal numNotaOrigem = notaOrigem.asBigDecimal("NUMNOTA");
        return "DEV REF NF " + (numNotaOrigem == null
                ? notaOrigem.asBigDecimal("NUNOTA").toPlainString()
                : numNotaOrigem.toPlainString());
    }

    private static BigDecimal asBigDecimal(Object valor) {
        if (valor == null) {
            return null;
        }
        if (valor instanceof BigDecimal) {
            return (BigDecimal) valor;
        }
        if (valor instanceof Number) {
            return new BigDecimal(valor.toString());
        }
        String texto = valor.toString().trim();
        return texto.isEmpty() ? null : new BigDecimal(texto);
    }

    private static BigDecimal nvl(BigDecimal valor, BigDecimal fallback) {
        return valor == null ? fallback : valor;
    }

    private static String nvl(String valor, String fallback) {
        return valor == null || valor.trim().isEmpty() ? fallback : valor;
    }

    private static String juntarNotas(List<BigDecimal> notas) {
        StringBuilder builder = new StringBuilder();

        for (BigDecimal nota : notas) {
            if (builder.length() > 0) {
                builder.append(", ");
            }
            builder.append(nota.toPlainString());
        }

        return builder.toString();
    }
}
