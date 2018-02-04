package com.sap.olingo.jpa.processor.core.query;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.server.api.ODataApplicationException;
import org.apache.olingo.server.api.uri.UriInfoResource;
import org.apache.olingo.server.api.uri.UriResource;
import org.apache.olingo.server.api.uri.UriResourceComplexProperty;
import org.apache.olingo.server.api.uri.UriResourceEntitySet;
import org.apache.olingo.server.api.uri.UriResourceNavigation;
import org.apache.olingo.server.api.uri.UriResourcePartTyped;
import org.apache.olingo.server.api.uri.UriResourceProperty;
import org.apache.olingo.server.api.uri.queryoption.ExpandOption;
import org.apache.olingo.server.api.uri.queryoption.SelectItem;
import org.apache.olingo.server.api.uri.queryoption.SelectOption;

import com.sap.olingo.jpa.metadata.core.edm.mapper.api.JPAAssociationPath;
import com.sap.olingo.jpa.metadata.core.edm.mapper.api.JPAAttribute;
import com.sap.olingo.jpa.metadata.core.edm.mapper.api.JPACollectionAttribute;
import com.sap.olingo.jpa.metadata.core.edm.mapper.api.JPAElement;
import com.sap.olingo.jpa.metadata.core.edm.mapper.api.JPAEntityType;
import com.sap.olingo.jpa.metadata.core.edm.mapper.api.JPAPath;
import com.sap.olingo.jpa.metadata.core.edm.mapper.api.JPAServiceDocument;
import com.sap.olingo.jpa.metadata.core.edm.mapper.api.JPAStructuredType;
import com.sap.olingo.jpa.metadata.core.edm.mapper.exception.ODataJPAModelException;
import com.sap.olingo.jpa.processor.core.exception.ODataJPAQueryException;
import com.sap.olingo.jpa.processor.core.exception.ODataJPAQueryException.MessageKeys;

public final class JPAExpandItemInfoFactory {

  private static final int ST_INDEX = 0;
  private static final int ET_INDEX = 1;
  private static final int PROPERTY_INDEX = 2;

  public List<JPAExpandItemInfo> buildExpandItemInfo(JPAServiceDocument sd, UriInfoResource uriResourceInfo,
      List<JPANavigationProptertyInfo> grandParentHops) throws ODataApplicationException {

    final List<JPAExpandItemInfo> itemList = new ArrayList<>();
    final List<UriResource> startResourceList = uriResourceInfo.getUriResourceParts();
    final ExpandOption expandOption = uriResourceInfo.getExpandOption();

    if (startResourceList != null && expandOption != null) {
      final List<JPANavigationProptertyInfo> parentHops = grandParentHops;
      final Map<JPAExpandItem, JPAAssociationPath> expandPath = Util.determineAssoziations(sd, startResourceList,
          expandOption);
      for (final Entry<JPAExpandItem, JPAAssociationPath> item : expandPath.entrySet()) {
        itemList.add(new JPAExpandItemInfo(sd, item.getKey(), item.getValue(), parentHops));
      }
    }
    return itemList;
  }

  /**
   * Navigate to collection property e.g. ../Organizations('1')/Comment
   * @param sd
   * @param uriResourceInfo
   * @param parentHops
   * @return
   * @throws ODataApplicationException
   */
  public List<JPACollectionItemInfo> buildCollectionItemInfo(final JPAServiceDocument sd,
      final UriInfoResource uriResourceInfo, final List<JPANavigationProptertyInfo> grandParentHops)
      throws ODataApplicationException {

    final List<JPACollectionItemInfo> itemList = new ArrayList<>();
    final List<UriResource> startResourceList = uriResourceInfo.getUriResourceParts();
    final SelectOption select = uriResourceInfo.getSelectOption();
    final JPAEntityType et = uriResourceInfo instanceof JPAExpandItem ? ((JPAExpandItem) uriResourceInfo)
        .getEntityType() : null;

    final Object[] types = determineNavigationElements(sd, startResourceList, et);
    try {
      if (types[PROPERTY_INDEX] != null) {
        if (((JPAPath) types[PROPERTY_INDEX]).getLeaf().isCollection()) {
          final JPACollectionExpandWrapper item = new JPACollectionExpandWrapper((JPAEntityType) types[ET_INDEX],
              uriResourceInfo);
          itemList.add(new JPACollectionItemInfo(sd, item, ((JPACollectionAttribute) ((JPAPath) types[PROPERTY_INDEX])
              .getLeaf()).asAssociation(), grandParentHops));
        }
      } else {

        if (select == null || select.getSelectItems().isEmpty() || select.getSelectItems().get(0).isStar()) {
          // No navigation, extract all collection attributes
          final JPAStructuredType st = (JPAStructuredType) types[ST_INDEX];
          final Set<JPAElement> collectionProperties = new HashSet<>();
          for (final JPAPath path : st.getPathList()) {
            for (final JPAElement pathElement : path.getPath()) {
              if (pathElement instanceof JPAAttribute && ((JPAAttribute) pathElement).isCollection()) {
                collectionProperties.add(pathElement);
                break;
              }
            }
          }
          for (final JPAElement pathElement : collectionProperties) {
            final JPACollectionExpandWrapper item = new JPACollectionExpandWrapper((JPAEntityType) types[ET_INDEX],
                uriResourceInfo);
            itemList.add(new JPACollectionItemInfo(sd, item, ((JPACollectionAttribute) pathElement)
                .asAssociation(), grandParentHops));
          }
        } else {
          final JPAStructuredType st = (JPAStructuredType) types[ST_INDEX];
          final List<JPAPath> selectOptions = getCollectionAttributesFromSelection(uriResourceInfo
              .getUriResourceParts(), st, uriResourceInfo.getSelectOption());
          for (JPAPath path : selectOptions) {
            final JPACollectionExpandWrapper item = new JPACollectionExpandWrapper((JPAEntityType) types[ET_INDEX],
                uriResourceInfo);
            itemList.add(new JPACollectionItemInfo(sd, item, ((JPACollectionAttribute) path.getLeaf())
                .asAssociation(), grandParentHops));
          }
        }
      }
    } catch (ODataJPAModelException e) {
      throw new ODataJPAQueryException(e, HttpStatusCode.INTERNAL_SERVER_ERROR);
    }
    return itemList;

  }

  private Object[] determineNavigationElements(final JPAServiceDocument sd,
      final List<UriResource> startResourceList, final JPAEntityType et) throws ODataJPAQueryException {

    Object[] result = new Object[3];
    if (startResourceList.isEmpty() && et != null) {
      result[ST_INDEX] = result[ET_INDEX] = et;
    } else {
      for (UriResource uriElement : startResourceList) {
        try {
          if (uriElement instanceof UriResourceEntitySet || uriElement instanceof UriResourceNavigation) {
            result[ST_INDEX] = result[ET_INDEX] = sd.getEntity(((UriResourcePartTyped) uriElement)
                .getType());
          } else if (uriElement instanceof UriResourceComplexProperty
              && !((UriResourceProperty) uriElement).isCollection())
            result[ST_INDEX] = sd.getComplexType(((UriResourceComplexProperty) uriElement).getComplexType());
          else if (uriElement instanceof UriResourceProperty
              && result[ST_INDEX] != null) {
            result[PROPERTY_INDEX] = ((JPAStructuredType) result[ST_INDEX]).getPath(((UriResourceProperty) uriElement)
                .getProperty().getName());
          }
        } catch (ODataJPAModelException e) {
          throw new ODataJPAQueryException(e, HttpStatusCode.INTERNAL_SERVER_ERROR);
        }
      }
    }
    return result;
  }

  protected List<JPAPath> getCollectionAttributesFromSelection(final List<UriResource> resources,
      final JPAStructuredType jpaEntity,
      final SelectOption select) throws ODataApplicationException, ODataJPAModelException {

    final List<JPAPath> collectionAttributes = new ArrayList<>();
    if (select == null || select.getSelectItems().isEmpty() || select.getSelectItems().get(0).isStar()) {
      for (final JPAPath selectItemPath : jpaEntity.getPathList()) {
        if (pathContainsCollection(selectItemPath))
          collectionAttributes.add(selectItemPath);
      }
    } else {
      final String pathPrefix = ""; // Util.determineProptertyNavigationPrefix(resources);
      for (SelectItem sItem : select.getSelectItems()) {
        final String pathItem = sItem.getResourcePath().getUriResourceParts().stream().map(path -> (path
            .getSegmentValue())).collect(Collectors.joining(JPAPath.PATH_SEPERATOR));
        final JPAPath selectItemPath = jpaEntity.getPath(pathPrefix.isEmpty() ? pathItem : pathPrefix + "/" + pathItem);
        if (selectItemPath == null)
          throw new ODataJPAQueryException(MessageKeys.QUERY_PREPARATION_INVALID_SELECTION_PATH,
              HttpStatusCode.BAD_REQUEST);
        if (pathContainsCollection(selectItemPath))
          collectionAttributes.add(selectItemPath);
      }
    }
    return collectionAttributes;
  }

  private boolean pathContainsCollection(final JPAPath p) {
    for (JPAElement pathElement : p.getPath()) {
      if (pathElement instanceof JPAAttribute && ((JPAAttribute) pathElement).isCollection()) {
        return true;
      }
    }
    return false;
  }
}
