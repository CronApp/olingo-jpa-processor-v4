package com.sap.olingo.jpa.processor.core.processor;

import java.util.Map;

import com.sap.olingo.jpa.metadata.core.edm.mapper.api.JPAEntityType;

public interface JPARequestLink {
  /**
   * Provides an instance of the entity metadata
   * @return
   */
  public JPAEntityType getEntityType();

  public Map<String, Object> getRelatedKeys();

  public Map<String, Object> getValues();
}
