package br.com.bela.sankhya.evento;

import br.com.sankhya.extensions.eventoprogramavel.EventoProgramavelJava;

/**
 * Alias com nome neutro para uso do mesmo evento em TGFCAB e TGFIXN.
 * Mantem compatibilidade com cadastros antigos que usam a classe legada.
 */
public class VinculaPedidoMarketplaceXml extends VinculaPedidoMarketplaceXmlTGFCAB implements EventoProgramavelJava {
}
