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
package org.castor.ddlgen;

import java.io.OutputStream;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.castor.ddlgen.schemaobject.Field;
import org.castor.ddlgen.schemaobject.ForeignKey;
import org.castor.ddlgen.schemaobject.KeyGenerator;
import org.castor.ddlgen.schemaobject.PrimaryKey;
import org.castor.ddlgen.schemaobject.Schema;
import org.castor.ddlgen.schemaobject.Table;
import org.castor.ddlgen.typeinfo.TypeInfo;
import org.exolab.castor.mapping.Mapping;
import org.exolab.castor.mapping.xml.ClassChoice;
import org.exolab.castor.mapping.xml.ClassMapping;
import org.exolab.castor.mapping.xml.FieldMapping;
import org.exolab.castor.mapping.xml.KeyGeneratorDef;
import org.exolab.castor.mapping.xml.MapTo;
import org.exolab.castor.mapping.xml.MappingRoot;
import org.exolab.castor.mapping.xml.Sql;

/**
 * AbstractGenerator is the base class for various DDL generator of specific DB and
 * handles following tasks: 
 * <li/> Extract information from Mapping to Schema
 * <li/> Loop through the schema and provide a skeleton for DDL creation
 * 
 * <p/>AbstractGenerator will automatically extract necessary information for DDL 
 * creation. That information is handled by Schema.
 * <p/>To create new generator for a DBMS, you should:
 * <li/> Overwrite this class to create new generator for a DBMS. 
 * <li/> If the syntax of DBMS is different to standard DDL syntax, you should 
 * overwrite SchemaObject (Table, Field, KeyGenerator, Index, ForeignKey,...) classes. 
 * The class SchemaObjectFactory who handles the SchemaObject creation must 
 * be overwritten.
 * <li/> You must overwrite the TypeMapper if mapping between JDBC types and 
 * specific DBMS�s types is different among various DBMS.
 * <p/>The example bellow shows how to create a generator for DB2:
 * <li/> <b>Generator for DB2</b>
 * <pre>
 *public class Db2Generator extends AbstractGenerator {
 *
 *    public Db2Generator(final String globConf, final String dbConf)
 *            throws GeneratorException {
 *        super(globConf, dbConf);
 *        setTypeMapper(new Db2TypeMapper(getConf()));
 *    }
 *}   
 * </pre>
 * <li/><b>TypeMapper for DB2</b>
 * <pre>
 *public final class Db2TypeMapper extends AbstractTypeMapper {
 *    public Db2TypeMapper(final Configuration conf) {
 *        super(conf);
 *    }
 * 
 *    protected void initialize(final Configuration conf) {
 *        // numeric types
 *        this.add(new NotSupportedType("bit"));
 *        LOG.warn("Db2 does not support 'TINY' type, use SMALLINT instead.");
 *        this.add(new NoParamType("tinyint", "SMALLINT"));
 *        this.add(new NoParamType("smallint", "SMALLINT"));
 *        this.add(new NoParamType("integer", "INTEGER"));
 *        this.add(new NoParamType("bigint", "BIGINT"));
 *    }
 *}
 *</pre>
 * <li/><b>Field for DB2</b>
 *<pre> 
 *public class Db2Field extends Field {
 *    public Db2Field() {
 *        super();
 *    }
 *
 *    public String toDDL() throws GeneratorException {
 *        StringBuffer buff = new StringBuffer();
 *        buff.append(getName()).append(" ");
 *        buff.append(getType().toDDL(this));
 *        
 *        if (isIdentity()) {
 *            buff.append(" NOT NULL");
 *        }
 *        
 *        KeyGenerator keyGen = getKeyGenerator();
 *        if (keyGen != null && isIdentity()) {
 *            
 *            if (KeyGenerator.IDENTITY_KEY.equalsIgnoreCase(keyGen.getName())) {
 *                buff.append(" GENERATED BY DEFAULT AS IDENTITY ").
 *                    append("START WITH 1 INCREMENT BY 1");
 *            }
 *        }
 *
 *        return buff.toString();
 *    }
 *}
 *</pre>
 * <li/><b>Field for DB2</b>
 *<pre> 
 *public class Db2SchemaFactory extends SchemaFactory {
 *    public Db2SchemaFactory() {
 *        super();
 *    }
 *    public Field createField() {
 *        return new Db2Field();
 *    }
 *
 *}
 *</pre>
 * The GeneratorFactory class handles the specific database generator creation. 
 * For example:
 * <pre>
 *  Generator generator = GeneratorFactory.
 *      createDDLGenerator(�mysql�, �ddl.properties�, �mysql.properties�);
 * </pre>
 *  
 * And to generate DDL, it should specify the printer and call generateDDL method.
 * <pre>
 *  generator.setPrinter(System.out);
 *  Mapping mapping = new Mapping();
 *  mapping.loadMapping("mapping.xml");
 *  generator.generateDDL(mapping);            
 * </pre>
 * 
 * @author <a href="mailto:leducbao AT gmail DOT com">Le Duc Bao</a>
 * @author <a href="mailto:ralf DOT joachim AT syscon DOT eu">Ralf Joachim</a>
 * @version $Revision: 8993 $ $Date: 2011-08-02 01:28:52 +0200 (Di, 02 Aug 2011) $
 * @since 1.1
 */
public abstract class AbstractGenerator implements Generator {
    //--------------------------------------------------------------------------

    /** handle all configurations (key, value). */
    private final DDLGenConfiguration _configuration;

    /** handle the key gen registry. */
    private KeyGeneratorRegistry _keyGenRegistry;

    /** handle the MappingHelper. */
    private MappingHelper _mappingHelper;

    /** handle the typemapper. */
    private TypeMapper _typeMapper;

    /** handle schema factory. */
    private SchemaFactory _schemaFactory;

    /** handle the _mapping document. */
    private Mapping _mapping;

    /** schema. */
    private Schema _schema;

    /** handle all resolving tables. */
    private final Map<String, ClassMapping> _resolveTable = new HashMap<String, ClassMapping>();

    //--------------------------------------------------------------------------

    /**
     * Constructor for AbstractGenerator.
     * 
     * @param configuration Configuration to use by the generator.
     */
    protected AbstractGenerator(final DDLGenConfiguration configuration) {
        _configuration = configuration;
    }
    
    //--------------------------------------------------------------------------

    /**
     * Get configuration of generator.
     * 
     * @return Configuration of generator.
     */
    public final DDLGenConfiguration getConfiguration() {
        return _configuration;
    }

    /**
     * Set key generator registry.
     * 
     * @param keyGenRegistry Key generator registry.
     */
    public final void setKeyGenRegistry(final KeyGeneratorRegistry keyGenRegistry) {
        _keyGenRegistry = keyGenRegistry;
    }

    /**
     * Set mapping helper.
     * 
     * @param mappingHelper Mapping helper.
     */
    protected final void setMappingHelper(final MappingHelper mappingHelper) {
        _mappingHelper = mappingHelper;
        _mappingHelper.setTypeMapper(_typeMapper);
    }

    /**
     * Get mapping helper.
     * 
     * @return Mapping helper.
     */
    public final MappingHelper getMappingHelper() {
        return _mappingHelper;
    }

    /**
     * Set type mapper.
     * 
     * @param typeMapper Type mapper.
     */
    public final void setTypeMapper(final TypeMapper typeMapper) {
        _typeMapper = typeMapper;
        _mappingHelper.setTypeMapper(_typeMapper);
    }

    /**
     * Get type mapper.
     * 
     * @return Type mapper.
     */
    public final TypeMapper getTypeMapper() {
        return _typeMapper;
    }

    /**
     * Set schema factory.
     * 
     * @param schemaFactory Schema factory.
     */
    protected final void setSchemaFactory(final SchemaFactory schemaFactory) {
        _schemaFactory = schemaFactory;
    }

    /**
     * Get schema factory.
     * 
     * @return Schema factory.
     */
    public final SchemaFactory getSchemaFactory() {
        return _schemaFactory;
    }

    /**
     * Set mapping document.
     * 
     * @param mapping Mapping document.
     */
    public final void setMapping(final Mapping mapping) {
        _mapping = mapping;
        _mappingHelper.setMapping(_mapping);
    }

    /**
     * Get mapping document.
     * 
     * @return Mapping document.
     */
    public final Mapping getMapping() {
        return _mapping;
    }

    /**
     * Get schema.
     * 
     * @return Schema
     */
    public final Schema getSchema() {
        return _schema;
    }

    //--------------------------------------------------------------------------

    /**
     * {@inheritDoc}
     */
    public final void generateDDL(final OutputStream output) throws GeneratorException {
        DDLWriter writer = new DDLWriter(output, _configuration);
        
        // Create schema.
        createSchema();
        
        // Generate DDL.
        String groupBy = _configuration.getStringValue(
                DDLGenConfiguration.GROUP_DDL_KEY,
                DDLGenConfiguration.GROUP_DDL_BY_TABLE);
        if (DDLGenConfiguration.GROUP_DDL_BY_TABLE.equalsIgnoreCase(groupBy)) {
            generateDDLGroupByTable(writer);
        } else if (DDLGenConfiguration.GROUP_DDL_BY_DDLTYPE.equalsIgnoreCase(groupBy)) {
            generateDDLGroupByDDLType(writer);
        } else {
            throw new GeneratorException("group ddl by do not support: " + groupBy);
        }
        
        writer.close();
    }

    /**
     * Generating ddl grouped by ddl type of DDL (e.g DROP, CREATE TABLE, create 
     * Primary key, create foreign key).
     * 
     * @param writer DDLWriter to write schema objects to.
     * @throws GeneratorException If failed to generate DDL.
     */
    private void generateDDLGroupByDDLType(final DDLWriter writer) throws GeneratorException {
        boolean genSchema = _configuration.getBoolValue(
                DDLGenConfiguration.GENERATE_DDL_FOR_SCHEMA_KEY, true);
        boolean genDrop = _configuration.getBoolValue(
                DDLGenConfiguration.GENERATE_DDL_FOR_DROP_KEY, true);
        boolean genCreate = _configuration.getBoolValue(
                DDLGenConfiguration.GENERATE_DDL_FOR_CREATE_KEY, true);
        boolean genPrimaryKey = _configuration.getBoolValue(
                DDLGenConfiguration.GENERATE_DDL_FOR_PRIMARYKEY_KEY, true);
        boolean genForeignKey = _configuration.getBoolValue(
                DDLGenConfiguration.GENERATE_DDL_FOR_FOREIRNKEY_KEY, true);
        boolean genIndex = _configuration.getBoolValue(
                DDLGenConfiguration.GENERATE_DDL_FOR_INDEX_KEY, true);
        boolean genKeyGen = _configuration.getBoolValue(
                DDLGenConfiguration.GENERATE_DDL_FOR_KEYGENERATOR_KEY, true);

        generateHeader(writer);

        //generate ddl for schema
        if (genSchema) { _schema.toCreateDDL(writer); }

        //generate drop statemetn
        if (genDrop) { generateDrop(writer); }

        //generate create statement
        if (genCreate) { generateCreate(writer); }

        //generate primary key creation statement
        if (genPrimaryKey) { generatePrimaryKey(writer); }

        //generate foreign key creation statement
        if (genForeignKey) { generateForeignKey(writer); }

        //generate index creation statement
        if (genIndex) { generateIndex(writer); }

        if (genKeyGen) { generateKeyGenerator(writer); }
    }

    /**
     * Generate DDL for drop statement of table.
     * 
     * @param writer DDLWriter to write schema objects to.
     * @throws GeneratorException If failed to generate DDL.
     */
    public final void generateDrop(final DDLWriter writer) throws GeneratorException {
        for (int i = 0; i < _schema.getTableCount(); i++) {
            _schema.getTable(i).toDropDDL(writer);
        }
    }

    /**
     * Generate DDL for create statementof table.
     * <pre>
     * CREATE TABLE prod (
     *  id INTEGER NOT NULL,
     *  name CHAR(16)
     * );
     * 
     * CREATE TABLE prod_detail (
     *  id INTEGER NOT NULL,
     *  prod_id CHAR(16)
     * );
     * </pre>
     * 
     * @param writer DDLWriter to write schema objects to.
     * @throws GeneratorException If failed to generate DDL.
     */
    public final void generateCreate(final DDLWriter writer) throws GeneratorException {
        for (int i = 0; i < _schema.getTableCount(); i++) {
            _schema.getTable(i).toCreateDDL(writer);
        }
    }

    /**
     * Generate DDL for primany keys.
     * 
     * @param writer DDLWriter to write schema objects to.
     * @throws GeneratorException If failed to generate DDL.
     */
    public final void generatePrimaryKey(final DDLWriter writer) throws GeneratorException {
        for (int i = 0; i < _schema.getTableCount(); i++) {
            _schema.getTable(i).getPrimaryKey().toCreateDDL(writer);
        }
    }

    /**
     * Generate DDL for foreign keys.
     * <pre>
     * ALTER TABLE `prod_group` ADD CONSTRAINT `FK_prod_group_1` 
     * FOREIGN KEY `FK_prod_group_1` (`id`, `name`)
     * REFERENCES `category` (`id`, `name`)
     * ON DELETE SET NULL
     * ON UPDATE CASCADE;
     * </pre>
     * 
     * @param writer DDLWriter to write schema objects to.
     * @throws GeneratorException If failed to generate DDL.
     */
    public final void generateForeignKey(final DDLWriter writer) throws GeneratorException {
        for (int i = 0; i < _schema.getTableCount(); i++) {
            createForeignKeyDDL(_schema.getTable(i), writer);
        }
    }

    /**
     * Generate DDL for indices.
     * 
     * @param writer DDLWriter to write schema objects to.
     * @throws GeneratorException If failed to generate DDL.
     */
    public final void generateIndex(final DDLWriter writer) throws GeneratorException {
        for (int i = 0; i < _schema.getTableCount(); i++) {
            createIndex(_schema.getTable(i), writer);
        }
    }

    /**
     * Generate DDL for key generators (sequence/trigger).
     * 
     * @param writer DDLWriter to write schema objects to.
     * @throws GeneratorException If failed to generate DDL.
     */
    public final void generateKeyGenerator(final DDLWriter writer) throws GeneratorException {
        for (int i = 0; i < _schema.getTableCount(); i++) {
            Table table = _schema.getTable(i);
            if (table.getKeyGenerator() != null) {
                table.getKeyGenerator().setTable(table);
                table.getKeyGenerator().toCreateDDL(writer);
            }
        }
    }

    /**
     * Generating ddl group by table.
     * 
     * @param writer DDLWriter to write schema objects to.
     * @throws GeneratorException If failed to generate DDL.
     */
    private void generateDDLGroupByTable(final DDLWriter writer) throws GeneratorException {
        boolean genSchema = _configuration.getBoolValue(
                DDLGenConfiguration.GENERATE_DDL_FOR_SCHEMA_KEY, true);
        boolean genDrop = _configuration.getBoolValue(
                DDLGenConfiguration.GENERATE_DDL_FOR_DROP_KEY, true);
        boolean genCreate = _configuration.getBoolValue(
                DDLGenConfiguration.GENERATE_DDL_FOR_CREATE_KEY, true);
        boolean genPrimaryKey = _configuration.getBoolValue(
                DDLGenConfiguration.GENERATE_DDL_FOR_PRIMARYKEY_KEY, true);
        boolean genForeignKey = _configuration.getBoolValue(
                DDLGenConfiguration.GENERATE_DDL_FOR_FOREIRNKEY_KEY, true);
        boolean genIndex = _configuration.getBoolValue(
                DDLGenConfiguration.GENERATE_DDL_FOR_INDEX_KEY, true);
        boolean genKeyGen = _configuration.getBoolValue(
                DDLGenConfiguration.GENERATE_DDL_FOR_KEYGENERATOR_KEY, true);

        generateHeader(writer);
        
        if (genSchema) { _schema.toCreateDDL(writer); }
        
        for (int i = 0; i < _schema.getTableCount(); i++) {
            Table table = _schema.getTable(i);

            if (genDrop) { table.toDropDDL(writer); }
            if (genCreate) { table.toCreateDDL(writer); }
            if (genPrimaryKey) { table.getPrimaryKey().toCreateDDL(writer); }
            if (genForeignKey) { createForeignKeyDDL(table, writer); }
            if (genIndex) { createIndex(table, writer); }
            if (genKeyGen && (table.getKeyGenerator() != null)) {
                table.getKeyGenerator().setTable(table);
                table.getKeyGenerator().toCreateDDL(writer);
            }
        }
    }

    /**
     * Generate DDL for foreign key.
     * 
     * @param table Table to generate DDL of foreign key for.
     * @param writer DDLWriter to write schema objects to.
     * @throws GeneratorException If failed to generate DDL.
     */
    protected final void createForeignKeyDDL(final Table table, final DDLWriter writer)
    throws GeneratorException {
        for (int i = 0; i < table.getForeignKeyCount(); i++) {
            table.getForeignKey(i).toCreateDDL(writer);
        }
    }

    /**
     * Generate DDL for indices of given table.
     * 
     * @param table Table to generate DDL of indices for.
     * @param writer DDLWriter to write schema objects to.
     * @throws GeneratorException If failed to generate DDL.
     */
    public final void createIndex(final Table table, final DDLWriter writer)
    throws GeneratorException {
        for (int i = 0; i < table.getIndexCount(); i++) {
            table.getIndex(i).toCreateDDL(writer);
        }
    }

    //--------------------------------------------------------------------------

    /**
     * Generate header comment.
     * 
     * @param writer DDLWriter to write schema objects to.
     */
    public abstract void generateHeader(final DDLWriter writer);

    //--------------------------------------------------------------------------

    /**
     * Extracting informations from mapping to schema, this is done by 3 steps.
     * <ul>
     *   <li>Create key generators</li>
     *   <li>Create tables</li>
     *   <li>Create additional tables for many-many relations</li>
     * </ul>
     * 
     * @throws GeneratorException If failed to create schema objects.
     */
    public final void createSchema() throws GeneratorException {
        // Create schema.
        MappingRoot root = _mapping.getRoot();
        _schema = _schemaFactory.createSchema();
        _schema.setConfiguration(_configuration);

        // Create key generators.
        Enumeration<? extends KeyGeneratorDef> ekg = root.enumerateKeyGeneratorDef();
        while (ekg.hasMoreElements()) {
            KeyGeneratorDef definition = ekg.nextElement();
            _keyGenRegistry.createKeyGenerator(definition);
        }

        // Create tables.
        Enumeration<? extends ClassMapping> ec = root.enumerateClassMapping();
        while (ec.hasMoreElements()) {
            ClassMapping cm = ec.nextElement();
            Table table = createTable(cm);
            if (table != null) { _schema.addTable(table); }
        }
        
        // Create N:M relation tables.
        Iterator<String> i = _resolveTable.keySet().iterator();
        while (i.hasNext()) {
            ClassMapping cm = _resolveTable.get(i.next());
            Table table = createTable(cm);
            if (table != null) { _schema.addTable(table); }
        }
    }

    /**
     * Create table from a ClassMapping.
     * 
     * @param cm ClassMapping.
     * @return Table schema object.
     * @throws GeneratorException If failed to create schema objects.
     */
    private Table createTable(final ClassMapping cm) throws GeneratorException {
        String tableName = cm.getMapTo().getTable();
        if (tableName == null) { return null; }

        Table table = _schemaFactory.createTable();
        table.setName(tableName);
        table.setConfiguration(_configuration);
        table.setSchema(_schema);
        
        PrimaryKey primarykey = _schemaFactory.createPrimaryKey();
        primarykey.setConfiguration(_configuration);
        primarykey.setTable(table);
        primarykey.setName("pk_" + tableName);
        table.setPrimaryKey(primarykey);

        // Return if there are no field in the table.
        if (cm.getClassChoice() == null) { return table; }

        boolean isUseFieldIdentity = _mappingHelper.isUseFieldIdentity(cm);
        Enumeration<? extends FieldMapping> ef = cm.getClassChoice().enumerateFieldMapping();

        // Process key generator.
        String keygenerator = cm.getKeyGenerator();
        KeyGenerator keyGen = null;
        if (keygenerator != null) {
            keyGen = _keyGenRegistry.getKeyGenerator(keygenerator.toUpperCase());
        }
        table.setKeyGenerator(keyGen);

        while (ef.hasMoreElements()) {
            FieldMapping fm = ef.nextElement();

            // Skip if <sql> tag is not defined and we have no mapping to DB.
            if (fm.getSql() == null) { continue; }

            boolean isFieldIdentity = fm.getIdentity();
            if (!isUseFieldIdentity) {
                isFieldIdentity = _mappingHelper.isIdentity(cm, fm);
            }
            
            // Checke for many-key, many-table definition.
            if (fm.getSql().getManyTable() != null) {
                    // Generate resolving table for many-many relationship
                    addResolveField(fm, cm);
            }
                    
            // Process column creation if sql name is defined.
            String[] sqlnames = fm.getSql().getName();              
            if ((sqlnames != null) && (sqlnames.length > 0)
                    && (fm.getSql().getManyTable() == null)) {
                // Normal case, using sql name as column name.
                String sqltype = fm.getSql().getType();

                TypeInfo typeInfo = null;
                ClassMapping cmRef = null;
                String[] refIdTypes = null;
                boolean isUseReferenceType = false;

                // Get type info.
                if (sqltype != null) {
                    typeInfo = _typeMapper.getType(sqltype);
                }

                // If typeInfo is null, this table has a reference to another one.
                if (typeInfo == null) {
                    cmRef = _mappingHelper.getClassMappingByName(fm.getType());
                    // Use field type if reference class could not be found.
                    if (cmRef == null) {                        
                        typeInfo = _typeMapper.getType(fm.getType());
                        
                        if (typeInfo == null) {
                            throw new TypeNotFoundException("can not resolve type "
                                + fm.getType() + " in class '" + cm.getName() + "'");
                        }
                    } else {
                        isUseReferenceType = true;
                        refIdTypes = _mappingHelper.resolveTypeReferenceForIds(
                                fm.getType());

                        // If number of reference table's Id's differ from number of
                        // field elements.
                        if (refIdTypes.length != sqlnames.length) {
                            throw new TypeNotFoundException(
                                    "number of reference table's Id differs"
                                            + " to number of field elements '"
                                            + fm.getName() + "' of class '" 
                                            + cm.getName() + "'"
                                            + refIdTypes.length + "," + sqlnames.length);
                        }
                    }
                }

                // Create fields.
                for (int i = 0; i < sqlnames.length; i++) {
                    Field field = _schemaFactory.createField();
                    field.setConfiguration(_configuration);

                    if (isUseReferenceType) {
                        // Each sqlname correspond to a identity of the reference table.
                        // Should be able to get the original type of the reference 
                        // field.
                        typeInfo = _typeMapper.getType(refIdTypes[i]);
                        if (typeInfo == null) {
                            throw new TypeNotFoundException(
                                    "can not find reference type "
                                    + refIdTypes[i] + " of class " + cm.getName());
                        }
                    }

                    // process attributes of field
                    field.setName(sqlnames[i]);
                    field.setTable(table);
                    field.setType(typeInfo);
                    field.setIdentity(isFieldIdentity);
                    field.setRequired(fm.getRequired());
                    field.setKeyGenerator(keyGen);

                    table.addField(field);
                    
                    if (isFieldIdentity) {
                       primarykey.addField(field);
                    }
                }
                
                // Create foreign keys.
                if (isUseReferenceType) { addOneOneForeignKey(table, fm); }
            }            
        }
        
        // Process extends, if extends is defined.
        processExtendedClass(table, cm);
        
        return table;
    }

    /**
     * Extract identities from extended ClassMapping and add them to table.
     * 
     * @param table Table to add identities of extended mapping to.
     * @param cm ClassMapping of table to extract get mappings from.
     * @throws GeneratorException throw exception if key-gen is not found.
     */
    private void processExtendedClass(final Table table, final ClassMapping cm)
    throws GeneratorException {
        Object extendClass = cm.getExtends();
        if (extendClass == null) { return; }
        
        ClassMapping extendCm = (ClassMapping) extendClass;
        String[] childIds = _mappingHelper.getClassMappingSqlIdentity(cm, false);
        
        if (childIds.length != 0) {
            // Check consistency.
            String[] childTypes = _mappingHelper.resolveTypeReferenceForIds(cm);
            String[] parentTypes = _mappingHelper.resolveTypeReferenceForIds(extendCm);
            
            if (childTypes.length != parentTypes.length) {
                throw new GeneratorException("Cannot resolve type for class '" 
                        + cm.getName() + "' from extend class '" 
                        + extendCm.getName() + "'");
            }
            for (int i = 0; i < childTypes.length; i++) {
                if (!childTypes[i].equalsIgnoreCase(parentTypes[i])) {
                    throw new GeneratorException("Cannot resolve type for class '" 
                            + cm.getName() + "' from extend class '" 
                            + extendCm.getName() + "'");
                }
            }
            return;
        }
        
        boolean isUseFieldIdentity = _mappingHelper.isUseFieldIdentity(extendCm);
        Enumeration<? extends FieldMapping> extendEf =
            extendCm.getClassChoice().enumerateFieldMapping();

        // Process key generator.
        String keygenerator = extendCm.getKeyGenerator();
        KeyGenerator keyGen = null;
        if (keygenerator != null) {
            keyGen = _keyGenRegistry.getKeyGenerator(keygenerator.toUpperCase());
        }
        table.setKeyGenerator(keyGen);

        while (extendEf.hasMoreElements()) {
            FieldMapping extendFm = extendEf.nextElement();

            // Skip if <sql> tag is not defined.
            if (extendFm.getSql() == null) { continue; }

            boolean isFieldIdentity = extendFm.getIdentity();
            if (!isUseFieldIdentity) {
                isFieldIdentity = _mappingHelper.isIdentity(extendCm, extendFm);
            }
            
            // Checke for many-key, many-table definition.
            if (isFieldIdentity && extendFm.getSql().getManyKeyCount() <= 0) {
                // Column is defiend as normal column in child, but it is id which is
                // inherited from parent.
                if (mergeIfDefInBothClasses(table, cm, extendFm)) { continue; }
                
                String[] sqlnames = extendFm.getSql().getName();
                String sqltype = extendFm.getSql().getType();

                TypeInfo typeInfo = null;
                ClassMapping cmRef = null;
                String[] refIdTypes = null;
                boolean isUseReferenceType = false;
                
                if (sqltype != null) {
                    typeInfo = _typeMapper.getType(sqltype);
                }

                // If typeInfo is null, this table has a reference to another one.
                if (typeInfo == null) {
                    cmRef = _mappingHelper.getClassMappingByName(extendFm.getType());
                    // If cmRef is null, the reference class could not be found.
                    if (cmRef == null) {                        
                        typeInfo = _typeMapper.getType(extendFm.getType());
                        
                        if (typeInfo == null) {
                            throw new TypeNotFoundException("can not resolve type "
                                + extendFm.getType());
                        }
                    } else {
                        isUseReferenceType = true;
                        refIdTypes = _mappingHelper.resolveTypeReferenceForIds(extendFm
                                .getType());

                        // If number of reference table's Ids differ from number of
                        // field elements.
                        if (refIdTypes.length != sqlnames.length) {
                            throw new TypeNotFoundException(
                                    "number of reference table's Id differs"
                                            + " to number of field elements '"
                                            + extendFm.getName() + "' of class '" 
                                            + extendCm.getName() + "'"
                                            + refIdTypes.length + "," + sqlnames.length);
                        }
                    }
                }

                // Create fields.
                for (int i = 0; i < sqlnames.length; i++) {
                    Field field = _schemaFactory.createField();
                    field.setConfiguration(_configuration);

                    if (isUseReferenceType) {
                        // Each sqlname is correspond to an identity of the reference
                        // table so, it should be possible to get the original type of
                        // the reference field.
                        typeInfo = _typeMapper.getType(refIdTypes[i]);
                        if (typeInfo == null) {
                            throw new TypeNotFoundException(
                                    "can not find reference type "
                                            + refIdTypes[i] + " of class "
                                            + extendCm.getName());
                        }
                    }

                    field.setName(sqlnames[i]);
                    field.setTable(table);
                    field.setType(typeInfo);
                    field.setIdentity(isFieldIdentity);                    
                    field.setKeyGenerator(keyGen);
                    
                    if (isFieldIdentity) {
                        table.getPrimaryKey().addField(field);
                    }

                    table.addField(field);
                }
            }
        }
        
        // Process extends.
        if (extendCm.getExtends() != null) {
            processExtendedClass(table, extendCm); 
        }
    }

    /**
     * This function is used to merge a table if it is mapped to many classes.
     * 
     * @param table Table to merge.
     * @param cm ClassMapping of table.
     * @param extendFm FieldMapping of extended class to be merged into table.
     * @return <code>true</code> if column is defiend as normal column in child, but is
     *         identity which is inherited from parent. 
     */
    private boolean mergeIfDefInBothClasses(final Table table, 
            final ClassMapping cm, final FieldMapping extendFm) {
        Enumeration<? extends FieldMapping> ef = cm.getClassChoice().enumerateFieldMapping();
        
        while (ef.hasMoreElements()) {
            FieldMapping fm = ef.nextElement();
            String fname = fm.getName();
            // If extend field has the same name with one of parent's fields.
            if (fname != null && fname.equalsIgnoreCase(extendFm.getName())) {
                if (fm.getSql() == null) { continue; }
                String[] sqlnames = fm.getSql().getName();
                for (int i = 0; i < sqlnames.length; i++) {
                    table.getField(sqlnames[i]).setIdentity(true);
                }
                return true;
            }
        }
        
        return false;
    }

    /**
     * Add foreign key for 1:1 relations to table schema object.
     * 
     * @param table Table to add foreign key to.
     * @param fm FieldMapping of relation.
     * @throws GeneratorException If failed to create foreign key schema object.
     */
    private void addOneOneForeignKey(final Table table, final FieldMapping fm)
    throws GeneratorException {
        ForeignKey fk = _schemaFactory.createForeignKey();
        fk.setConfiguration(_configuration);

        fk.setTable(table);
        fk.setName(table.getName() + "_" + fm.getName());
        
        String[] fieldNames = fm.getSql().getName();
        for (int i = 0; i < fieldNames.length; i++) {
            for (int j = 0; j < table.getFieldCount(); j++) {
                Field field = table.getField(j);
                if (fieldNames[i].equals(field.getName())) { fk.addField(field); }
            }
        }

        ClassMapping cm = _mappingHelper.getClassMappingByName(fm.getType());

        if (cm == null) {
            throw new GeneratorException("can not find class " + fm.getType());
        }
        
        String referenceTableName = cm.getMapTo().getTable();
        Table referenceTable = null;
        referenceTable = table.getSchema().getTable(referenceTableName);
        fk.setReferenceTable(referenceTable);
        
        String[] manykeys = fm.getSql().getManyKey();
        if (manykeys == null || manykeys.length == 0) {
            manykeys = _mappingHelper.getClassMappingSqlIdentity(cm, true);
        }
        for (int i = 0; i < manykeys.length; i++) {
            for (int j = 0; j < referenceTable.getFieldCount(); j++) {
                Field field = referenceTable.getField(j);
                if (manykeys[i].equals(field.getName())) { fk.addReferenceField(field); }
            }
        }

        fk.setRelationType(ForeignKey.ONE_ONE);
        table.addForeignKey(fk);
    }

    /**
     * Add column for a resolving table which is required by M:N relationship.
     * 
     * @param fm FieldMapping.
     * @param cm ClassMapping.
     */
    private void addResolveField(final FieldMapping fm, final ClassMapping cm) {
        String keyGen = cm.getKeyGenerator();
        ClassMapping resolveCm = null;
        
        // Get table, if not existe, create one.
        if (_resolveTable.containsKey(fm.getSql().getManyTable())) {
            resolveCm = _resolveTable.get(fm.getSql().getManyTable());
        } else {
            resolveCm = new ClassMapping();
            resolveCm.setName(fm.getSql().getManyTable());
            resolveCm.setKeyGenerator(keyGen);

            MapTo mapto = new MapTo();
            mapto.setTable(fm.getSql().getManyTable());
            resolveCm.setMapTo(mapto);
            _resolveTable.put(fm.getSql().getManyTable(), resolveCm);
        }

        FieldMapping resolveFm = new FieldMapping();
        resolveFm.setIdentity(true);
        resolveFm.setName(cm.getMapTo().getTable());
        resolveFm.setType(cm.getName());

        ClassChoice cc = resolveCm.getClassChoice();
        if (cc == null) {
            cc = new ClassChoice();
            resolveCm.setClassChoice(cc); 
        }
        cc.addFieldMapping(resolveFm);

        Sql sql = new Sql();
        String[] sqlname = fm.getSql().getManyKey();
        if (sqlname == null || sqlname.length == 0) {
            _mappingHelper.getClassMappingSqlIdentity(cm, true);
        }
        sql.setName(sqlname);
        resolveFm.setSql(sql);
    }

    //--------------------------------------------------------------------------
}
