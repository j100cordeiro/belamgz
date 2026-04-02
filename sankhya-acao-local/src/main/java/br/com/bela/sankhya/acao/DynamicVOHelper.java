package br.com.bela.sankhya.acao;

import br.com.sankhya.jape.EntityFacade;
import br.com.sankhya.jape.dao.EntityDAO;
import br.com.sankhya.jape.dao.EntityPropertyDescriptor;
import br.com.sankhya.jape.dao.PersistentObjectUID;
import br.com.sankhya.jape.bmp.PersistentLocalEntity;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.vo.EntityVO;

import java.util.Map;

final class DynamicVOHelper {

    private DynamicVOHelper() {
    }

    static DynamicVO duplicarCabecalhoNota(EntityFacade entityFacade, DynamicVO origemVO, Map<String, Object> campos)
            throws Exception {
        DynamicVO destinoVO = origemVO.buildClone();

        destinoVO.setProperty("NUNOTA", null);
        destinoVO.setProperty("DHTIPOPER", null);
        destinoVO.setProperty("DHTIPVENDA", null);

        for (Map.Entry<String, Object> entry : campos.entrySet()) {
            destinoVO.setProperty(entry.getKey(), entry.getValue());
        }

        PersistentLocalEntity entidadeCriada = entityFacade.createEntity("CabecalhoNota", (EntityVO) destinoVO);
        return (DynamicVO) entidadeCriada.getValueObject();
    }

    static DynamicVO duplicarItemNota(EntityFacade entityFacade, DynamicVO origemVO, Map<String, Object> campos)
            throws Exception {
        DynamicVO destinoVO = origemVO.buildClone();
        EntityDAO itemDao = entityFacade.getDAOInstance("ItemNota");
        limparPk(destinoVO, itemDao);

        for (Map.Entry<String, Object> entry : campos.entrySet()) {
            destinoVO.setProperty(entry.getKey(), entry.getValue());
        }

        PersistentLocalEntity entidadeCriada = entityFacade.createEntity("ItemNota", (EntityVO) destinoVO);
        return (DynamicVO) entidadeCriada.getValueObject();
    }

    private static void limparPk(DynamicVO dynamicVO, EntityDAO dao) throws Exception {
        PersistentObjectUID objectUID = dao.getSQLProvider().getPkObjectUID();
        EntityPropertyDescriptor[] pkFields = objectUID.getFieldDescriptors();

        for (EntityPropertyDescriptor pkField : pkFields) {
            dynamicVO.setProperty(pkField.getField().getName(), null);
        }
    }
}
