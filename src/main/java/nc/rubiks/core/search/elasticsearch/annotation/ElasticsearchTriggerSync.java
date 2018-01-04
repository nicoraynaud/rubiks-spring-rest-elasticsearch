package nc.rubiks.core.search.elasticsearch.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation for marking fields of entities that needs
 * to be synced when the parent entity is being synced.
 *
 * In order to work properly, the associated object must be
 * annotated with @ElasticsearchDocument and have an id
 * of type Long accessible through "getId()" method
 *
 * @author nicoraynaud
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface ElasticsearchTriggerSync {

}
