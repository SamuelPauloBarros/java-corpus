/*
 * Copyright 2006 Le Duc Bao, Ralf Joachim
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.castor.ddlgen.engine.mysql;

import org.castor.ddlgen.Configuration;
import org.castor.ddlgen.DDLGenConfiguration;
import org.castor.ddlgen.DDLWriter;
import org.castor.ddlgen.GeneratorException;
import org.castor.ddlgen.schemaobject.Table;

/**
 * MySQL table.
 * 
 * @author <a href="mailto:leducbao AT gmail DOT com">Le Duc Bao</a>
 * @author <a href="mailto:ralf DOT joachim AT syscon DOT eu">Ralf Joachim</a>
 * @version $Revision: 8993 $ $Date: 2011-08-02 01:28:52 +0200 (Di, 02 Aug 2011) $
 * @since 1.1
 */
public final class MysqlTable extends Table {
    //--------------------------------------------------------------------------

    /**
     * {@inheritDoc}
     */
    public void toCreateDDL(final DDLWriter writer) throws GeneratorException {
        Configuration conf = getConfiguration();
        String engine = conf.getStringValue(DDLGenConfiguration.STORAGE_ENGINE_KEY, null);
        String delimiter = DDLGenConfiguration.DEFAULT_STATEMENT_DELIMITER;
        
        writer.println();
        writer.println();
        writer.println("CREATE TABLE {0} (", new Object[] {getName()});
        fields(writer);
        writer.println();
        if ((engine != null) && !"".equals(engine)) {
            writer.print(") ENGINE={0}{1}", new Object[] {engine, delimiter});
        } else {
            writer.print("){0}", new Object[] {delimiter});
        }
    }

    /**
     * {@inheritDoc}
     */
    public void toDropDDL(final DDLWriter writer) {
        String delimiter = DDLGenConfiguration.DEFAULT_STATEMENT_DELIMITER;

        writer.println();
        writer.println();
        writer.print("DROP TABLE IF EXISTS {0}{1}", new Object[] {getName(), delimiter});
    }

    //--------------------------------------------------------------------------
}
