/*
 * Copyright 2006 Ralf Joachim
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.castor.cache.hashbelt.reaper;

import org.castor.cache.hashbelt.container.Container;

/**
 * Mock object for testing of AbstractReaper.
 * 
 * @author <a href="mailto:ralf DOT joachim AT syscon DOT eu">Ralf Joachim</a>
 * @version $Revision: 9041 $ $Date: 2011-08-16 11:51:17 +0200 (Di, 16 Aug 2011) $
 * @since 1.0
 */
public class AbstractReaperMock<K, V> extends AbstractReaper<K, V> {
    public void handleExpiredContainer(final Container<K, V> expiredContainer) { }
}
