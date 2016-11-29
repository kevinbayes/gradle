/*
 * Copyright 2012 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle.api.internal.component;

import org.gradle.api.Nullable;
import org.gradle.api.component.CompositeSoftwareComponent;
import org.gradle.api.component.ConsumableSoftwareComponent;
import org.gradle.api.component.SoftwareComponent;
import org.gradle.api.component.SoftwareComponentContainer;
import org.gradle.api.internal.DefaultPolymorphicDomainObjectContainer;
import org.gradle.internal.reflect.Instantiator;

public class DefaultSoftwareComponentContainer extends DefaultPolymorphicDomainObjectContainer<SoftwareComponent> implements SoftwareComponentContainer {
    public DefaultSoftwareComponentContainer(Instantiator instantiator) {
        super(SoftwareComponent.class, instantiator);
        // TODO:ADAM - inject a registry instead to avoid creating many factory instances
        registerFactory(CompositeSoftwareComponent.class, new CompositeSoftwareComponentFactory(instantiator));
        registerFactory(ConsumableSoftwareComponent.class, new ConsumableSoftwareComponentFactory());
    }

}
