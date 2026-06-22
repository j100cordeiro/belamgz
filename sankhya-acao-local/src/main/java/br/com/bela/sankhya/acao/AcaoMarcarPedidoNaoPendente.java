package br.com.bela.sankhya.acao;

import br.com.sankhya.extensions.actionbutton.AcaoRotinaJava;
import br.com.sankhya.extensions.actionbutton.ContextoAcao;
import br.com.sankhya.extensions.actionbutton.Registro;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.wrapper.JapeFactory;
import br.com.sankhya.jape.wrapper.JapeWrapper;
import br.com.sankhya.jape.wrapper.fluid.FluidUpdateVO;

import java.math.BigDecimal;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Botao de acao para TGFCAB/CabecalhoNota.
 * Marca o pedido selecionado como nao pendente no cabecalho.
 */
public class AcaoMarcarPedidoNaoPendente implements AcaoRotinaJava {

    @Override
    public void doAction(ContextoAcao contexto) throws Exception {
        Registro[] linhas = contexto.getLinhas();
        if (linhas == null || linhas.length == 0) {
            contexto.mostraErro("Selecione ao menos um pedido.");
            return;
        }

        JapeWrapper cabecalhoDao = JapeFactory.dao("CabecalhoNota");
        Set<BigDecimal> notas = coletarNotasSelecionadas(linhas, contexto);
        int atualizadas = 0;
        int jaNaoPendentes = 0;

        for (BigDecimal nunota : notas) {
            DynamicVO cabecalho = cabecalhoDao.findOne("NUNOTA = ?", nunota);
            if (cabecalho == null) {
                contexto.mostraErro("Nao foi possivel localizar o pedido " + nunota.toPlainString() + ".");
                return;
            }

            validarPedido(cabecalho, contexto);

            if ("N".equals(cabecalho.asString("PENDENTE"))) {
                jaNaoPendentes++;
                continue;
            }

            FluidUpdateVO updateVO = cabecalhoDao.prepareToUpdate(cabecalho);
            updateVO.set("PENDENTE", "N");
            updateVO.update();
            atualizadas++;
        }

        contexto.setMensagemRetorno(montarMensagem(atualizadas, jaNaoPendentes, notas.size()));
    }

    private Set<BigDecimal> coletarNotasSelecionadas(Registro[] linhas, ContextoAcao contexto) throws Exception {
        Set<BigDecimal> notas = new LinkedHashSet<BigDecimal>();

        for (Registro linha : linhas) {
            BigDecimal nunota = asBigDecimal(linha.getCampo("NUNOTA"));
            if (nunota == null) {
                contexto.mostraErro("Nao foi possivel identificar a NUNOTA de uma das linhas selecionadas.");
                return notas;
            }

            notas.add(nunota);
        }

        return notas;
    }

    private void validarPedido(DynamicVO cabecalho, ContextoAcao contexto) throws Exception {
        BigDecimal nunota = cabecalho.asBigDecimal("NUNOTA");
        String tipMov = cabecalho.asString("TIPMOV");

        if (!"P".equals(tipMov)) {
            contexto.mostraErro("A nota " + nunota.toPlainString() + " nao e um pedido. TIPMOV atual: "
                    + valorOuVazio(tipMov) + ".");
        }
    }

    private String montarMensagem(int atualizadas, int jaNaoPendentes, int totalSelecionadas) {
        if (atualizadas == totalSelecionadas) {
            return "Pedido(s) marcado(s) como nao pendente(s): " + atualizadas + ".";
        }

        if (atualizadas == 0 && jaNaoPendentes > 0) {
            return "Todos os pedidos selecionados ja estavam como nao pendentes.";
        }

        return "Pedido(s) marcado(s) como nao pendente(s): " + atualizadas
                + ". Ja nao pendente(s): " + jaNaoPendentes + ".";
    }

    private BigDecimal asBigDecimal(Object valor) {
        if (valor == null) {
            return null;
        }

        if (valor instanceof BigDecimal) {
            return (BigDecimal) valor;
        }

        if (valor instanceof Number) {
            return BigDecimal.valueOf(((Number) valor).longValue());
        }

        String texto = valor.toString().trim();
        if (texto.isEmpty()) {
            return null;
        }

        return new BigDecimal(texto);
    }

    private String valorOuVazio(String valor) {
        return valor == null || valor.trim().isEmpty() ? "<vazio>" : valor;
    }
}
