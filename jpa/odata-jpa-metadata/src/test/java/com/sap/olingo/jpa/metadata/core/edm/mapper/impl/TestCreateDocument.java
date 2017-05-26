package com.sap.olingo.jpa.metadata.core.edm.mapper.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.List;

import org.apache.olingo.commons.api.edm.provider.CsdlEntityContainer;
import org.apache.olingo.commons.api.edm.provider.CsdlSchema;
import org.junit.Test;

import com.sap.olingo.jpa.metadata.core.edm.mapper.api.JPAServiceDocument;
import com.sap.olingo.jpa.metadata.core.edm.mapper.exception.ODataJPAModelException;

public class TestCreateDocument extends TestMappingRoot {

  @Test
  public void checkServiceDocumentCanBeCreated() throws ODataJPAModelException {
    new IntermediateServiceDocument(PUNIT_NAME, emf.getMetamodel(), null, null);
  }

  @Test
  public void checkServiceDocumentGetSchemaList() throws ODataJPAModelException {
    JPAServiceDocument svc = new IntermediateServiceDocument(PUNIT_NAME, emf.getMetamodel(), null, null);
    assertEquals("Wrong number of schemas", 1, svc.getEdmSchemas().size());
  }

  @Test
  public void checkServiceDocumentGetContainer() throws ODataJPAModelException {
    JPAServiceDocument svc = new IntermediateServiceDocument(PUNIT_NAME, emf.getMetamodel(), null, null);
    assertNotNull("Entity Container not found", svc.getEdmEntityContainer());
  }

  @Test
  public void checkServiceDocumentGetContainerFromSchema() throws ODataJPAModelException {
    JPAServiceDocument svc = new IntermediateServiceDocument(PUNIT_NAME, emf.getMetamodel(), null, null);
    List<CsdlSchema> schemas = svc.getEdmSchemas();
    CsdlSchema schema = schemas.get(0);
    assertNotNull("Entity Container not found", schema.getEntityContainer());
  }

  @Test
  public void checkServiceDocumentGetEntitySetsFromContainer() throws ODataJPAModelException {
    JPAServiceDocument svc = new IntermediateServiceDocument(PUNIT_NAME, emf.getMetamodel(), null, null);
    CsdlEntityContainer container = svc.getEdmEntityContainer();
    assertNotNull("Entity Container not found", container.getEntitySets());
  }

}
