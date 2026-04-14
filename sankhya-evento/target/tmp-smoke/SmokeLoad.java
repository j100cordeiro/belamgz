import br.com.bela.sankhya.evento.VinculaPedidoMarketplaceXmlTGFCAB;
import br.com.sankhya.jape.event.PersistenceEvent;

public class SmokeLoad {
    public static void main(String[] args) throws Exception {
        VinculaPedidoMarketplaceXmlTGFCAB ev = new VinculaPedidoMarketplaceXmlTGFCAB();
        ev.afterInsert(new PersistenceEvent());
        ev.afterUpdate(new PersistenceEvent());
        System.out.println("OK_SMOKE");
    }
}
