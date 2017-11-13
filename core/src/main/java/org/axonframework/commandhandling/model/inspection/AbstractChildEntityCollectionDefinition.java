/*
 * Copyright (c) 2010-2017. Axon Framework
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.axonframework.commandhandling.model.inspection;

import org.axonframework.common.AxonConfigurationException;
import org.axonframework.common.property.Property;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static org.axonframework.common.ObjectUtils.getOrDefault;
import static org.axonframework.common.property.PropertyAccessStrategy.getProperty;

/**
 * Abstract implementation of the {@link org.axonframework.commandhandling.model.inspection.ChildEntityDefinition} to
 * provide reusable functionality for collections of ChildEntityDefinitions.
 */
public abstract class AbstractChildEntityCollectionDefinition implements ChildEntityDefinition {

    /**
     * Resolves the type of the Child Entity, either by pulling it from the {@link org.axonframework.commandhandling.model.AggregateMember}
     * its attributes, or by resolving it him self through the {@link AbstractChildEntityCollectionDefinition#resolveType(Map,
     * Field)} function.
     *
     * @param attributes a {@link java.util.Map} of key/value types {@link java.lang.String}/{@link java.lang.Object}
     *                   containing the attributes of the {@link org.axonframework.commandhandling.model.AggregateMember}
     *                   annotation.
     * @param field      a {@link java.lang.reflect.Field} denoting the Child Entity to resolve the type of.
     * @return the type as a {@link java.lang.Class} of the Child Entity.
     */
    protected Class<?> resolveType(Map<String, Object> attributes, Field field) {
        Class<?> entityType = (Class<?>) attributes.get("type");
        if (Void.class.equals(entityType)) {
            entityType = resolveGenericType(field).orElseThrow(() -> new AxonConfigurationException(format(
                    "Unable to resolve entity type of field [%s]. Please provide type explicitly in @AggregateMember annotation.",
                    field.toGenericString()
            )));
        }

        return entityType;
    }

    /**
     * Resolves the generic type of a {@link java.lang.reflect.Field} Child Entity.
     *
     * @param field a {@link java.lang.reflect.Field} denoting the Child Entity to resolve the type of.
     * @return the type as a {@link java.lang.Class} of the given {@code field}.
     */
    protected abstract Optional<Class<?>> resolveGenericType(Field field);

    /**
     * Retrieves the routing keys of every command handler on the given {@code childEntityModel} to be able to correctly
     * route commands to Entities.
     *
     * @param field            a {@link java.lang.reflect.Field} denoting the Child Entity upon which the {@code
     *                         childEntityModel} is based.
     * @param childEntityModel a {@link org.axonframework.commandhandling.model.inspection.EntityModel} to retrieve the
     *                         routing key properties from.
     * @return a {@link java.util.Map} of key/value types {@link java.lang.String}/{@link
     * org.axonframework.common.property.Property} from Command Message name to routing key.
     */
    protected Map<String, Property<Object>> extractCommandHandlerRoutingKeys(Field field,
                                                                             EntityModel<Object> childEntityModel) {
        return childEntityModel.commandHandlers()
                               .values()
                               .stream()
                               .map(commandHandler -> commandHandler.unwrap(CommandMessageHandlingMember.class)
                                                                    .orElse(null))
                               .filter(Objects::nonNull)
                               .collect(Collectors.toMap(
                                       CommandMessageHandlingMember::commandName,
                                       commandHandler -> extractCommandHandlerRoutingKey(childEntityModel,
                                                                                         commandHandler,
                                                                                         field
                                       )
                               ));
    }

    @SuppressWarnings("unchecked")
    private Property<Object> extractCommandHandlerRoutingKey(EntityModel<Object> childEntityModel,
                                                             CommandMessageHandlingMember commandHandler,
                                                             Field field) {
        String routingKey = getOrDefault(commandHandler.routingKey(), childEntityModel.routingKey());

        Property<Object> property = getProperty(commandHandler.payloadType(), routingKey);

        if (property == null) {
            throw new AxonConfigurationException(format(
                    "Command of type [%s] doesn't have a property matching the routing key [%s] necessary to route through field [%s]",
                    commandHandler.payloadType(),
                    routingKey,
                    field.toGenericString())
            );
        }
        return property;
    }
}
