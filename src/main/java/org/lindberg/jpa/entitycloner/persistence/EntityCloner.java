package org.lindberg.jpa.entitycloner.persistence;


import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.persistence.Embeddable;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;

import org.apache.commons.lang3.StringUtils;
import org.lindberg.jpa.entitycloner.util.ReflectionUtil;


/**
 * 
 * Cloner of persistent entities. Clone a persistent entity and every tree of relationships, based on jpa annotations.
 *   
 * @author Victor Lindberg (victorlindberg713@gmail.com)
 * 
 */
@SuppressWarnings("rawtypes")
public class EntityCloner<E> {
	
	/**
	 * Original entity to clone.
	 */
	private E entity;
	
	/**
	 * Create a EntityCloner.
	 * @param entity original entity to clone.
	 */
	public EntityCloner(E entity) {
		this.entity = entity;
	}
	
	/**
	 * Generate a clone of the original entity;
	 * 
	 */
	@SuppressWarnings("unchecked")
	public E generateClone() {
        return  (E) generateCopyToPersist(entity, new HashMap<Object, Object>());
    }
	
	/**
     * Gera uma copia da entidade persistente pronta para para a inclusão.
     * 
     * @param entity entitade persistente a ser copiada.
     * @param entityCache cache das entidades que já foram copiados de modo que quando algum bean for passado para ser
     *            copiado antes da copia for feita o cache é verificado para ver se o bean ja foi copiado e
     *            assim essa instancia de copia ser usada ao invés de fazer novamente a copia.
     * @return copia da entidade pronta para para a inclusão.
     */
	protected Object generateCopyToPersist(Object entity,
        Map<Object, Object> entityCache) {
    	Object cacheCopy = entityCache.get(entity);
        if (cacheCopy != null)
            return cacheCopy;

        Object entityCopy = ReflectionUtil.createInstance(entity.getClass());
        entityCache.put(entity, entityCopy);

        Field[] fields = ReflectionUtil.getFields(entity, true, true);

        for (Field entityField : fields) {
            int modifiers = entityField.getModifiers();
            IgnoreClone noPersistenceCopyAnnot = entityField.getAnnotation(IgnoreClone.class);
            if (! entityField.isAnnotationPresent(ForceClone.class) && entityField.isAnnotationPresent(Id.class) || Modifier.isFinal(modifiers) ||
            	(noPersistenceCopyAnnot != null && noPersistenceCopyAnnot.setNull()))
                continue;

            try {
                ReflectionUtil.makeAttributesAccessible(entityField);
                Object fieldValue = entityField.get(entity);
                if (isEntity(fieldValue) && 
                		(entityField.isAnnotationPresent(ForceClone.class) || (! entityField.isAnnotationPresent(ManyToOne.class) && 
                		! isNoCopyField(entity.getClass(), entityField)))){
                	Object fieldValueCopy = generateCopyToPersist(fieldValue, entityCache);
                	entityCache.put(fieldValue, fieldValueCopy);
                	Field oneToOneBack = getBackFieldRelationship(entityCopy, fieldValue, entityField, OneToOne.class, OneToOne.class);
                	if (oneToOneBack != null){
                		ReflectionUtil.setValueByField(oneToOneBack, fieldValueCopy, entityCopy);
                	}
                	fieldValue = fieldValueCopy;
                }else
                   if (fieldValue instanceof Collection) {
                      boolean oneToManyRelationship = entityField.isAnnotationPresent(OneToMany.class);
                      fieldValue = generateCopyCollectionToPersist(entityCopy, (Collection) fieldValue, entityField,oneToManyRelationship, entityCache);
                   }
                
                entityField.set(entityCopy, fieldValue);
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }

        return entityCopy;
    }
    
    /**
     * Gera uma copia da coleção de entidades pronta para inclusão.
     * 
     * @param entityCopy copia da entidade onde a copia da coleção será setada.
     * @param collection coleção a ser copiada.
     * @param entityField 
     * @param oneToManyRelationship true se o campo corresponde é um atributo de relacionamento 1 x n dentro do mapeamento.
     * @param entityCache cache das entidades que já foram copiados de modo que quando algum bean for passado para ser
     *            copiado antes da copia for feita o cache é verificado para ver se o bean ja foi copiado e
     *            assim essa instancia de copia ser usada ao invés de fazer novamente a copia.
     * @return copia da entidade pronta para para a inclusão.
     * @throws IllegalArgumentException 
     * @throws IllegalAccessException
     */
    @SuppressWarnings("unchecked")
	protected Collection generateCopyCollectionToPersist(Object entityCopy, Collection collection,
                Field entityField, boolean oneToManyRelationship, Map<Object, Object> entityCache)
        throws IllegalArgumentException, IllegalAccessException {
    	
    	Collection collectionCopy;
        if (collection instanceof List) {
            collectionCopy = new ArrayList();
        } else
        	if (collection instanceof Set)
        		collectionCopy = new HashSet();
        	else
        		collectionCopy = (Collection) ReflectionUtil.createInstance(collection.getClass());
        
        if (oneToManyRelationship) {
            for (Object item : collection) {
                if (isEntity(item)) {
                	Object itemCopy = generateCopyToPersist(item, entityCache);
                    Field relationshipBackField = getBackFieldRelationship(entityCopy, itemCopy, entityField, OneToMany.class, ManyToOne.class);
                    if (relationshipBackField == null)
                    	relationshipBackField = getBackFieldRelationship(entityCopy, itemCopy, entityField,OneToOne.class, OneToOne.class);
                    
                    if (relationshipBackField != null){
                    	ReflectionUtil.makeAttributesAccessible(relationshipBackField);
                        relationshipBackField.set(itemCopy, entityCopy);
                    }
                    collectionCopy.add(itemCopy);
                }

            }
        } else
            collectionCopy.addAll(collection);

        return collectionCopy;
    }
    
    protected Field getBackFieldRelationship(Object entityCopy, Object entity, Field forwardField, Class<? extends Annotation> forwardRelationshipType, 
    		Class<? extends Annotation> backRelationshipType) {
        Field[] fields = ReflectionUtil.getFields(entity, true, true);
        for (Field field : fields) {
           if (field.isAnnotationPresent(backRelationshipType) && field.getType().isAssignableFrom(entityCopy.getClass())){
        	   String mappedBy = null;
        	   if(forwardRelationshipType.equals(OneToMany.class)){
        		   OneToMany oneToManyForward = forwardField.getAnnotation(OneToMany.class);
        		   if(oneToManyForward != null)
        			   mappedBy = oneToManyForward.mappedBy();
        	   }else
        		   if(forwardRelationshipType.equals(OneToOne.class)){
        			   OneToOne oneToOneForward = forwardField.getAnnotation(OneToOne.class);
            		   if(oneToOneForward != null)
            			   mappedBy = oneToOneForward.mappedBy();
        		   }
        	   
        	  if(StringUtils.isNotBlank(mappedBy) && mappedBy.equals(field.getName()))
        		  return field;
              
           }
        }

        return null;
    }
    
    protected boolean isEntity(Object bean){
    	return bean != null && (bean.getClass().isAnnotationPresent(Entity.class)
    			|| bean.getClass().isAnnotationPresent(Embeddable.class));
    }
    
    protected boolean isNoCopyField(Class clazz, Field field){
    	IgnoreClone noPersistenceCopy = field.getAnnotation(IgnoreClone.class);
    	if (noPersistenceCopy != null){
    		for (Class<?> forClazz : noPersistenceCopy.forInheritedClasses())
    			if (forClazz.isAssignableFrom(clazz))
    				return true;
    	}
    	
    	return false;
    }
    
	
}