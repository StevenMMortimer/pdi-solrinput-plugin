/*! ******************************************************************************
*
* Pentaho Data Integration
*
* Copyright (C) 2002-2013 by Pentaho : http://www.pentaho.com
*
*******************************************************************************
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with
* the License. You may obtain a copy of the License at
*
*    http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*
******************************************************************************/

package org.pentaho.di.trans.steps.solrinput;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import org.pentaho.di.core.CheckResult;
import org.pentaho.di.core.CheckResultInterface;
import org.pentaho.di.core.Const;
import org.pentaho.di.core.annotations.Step;
import org.pentaho.di.core.database.DatabaseMeta;
import org.pentaho.di.core.encryption.Encr;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.exception.KettleValueException;
import org.pentaho.di.core.exception.KettleXMLException;
import org.pentaho.di.core.row.RowMetaInterface;
import org.pentaho.di.core.row.ValueMeta;
import org.pentaho.di.core.row.ValueMetaInterface;
import org.pentaho.di.core.row.value.ValueMetaBase;
import org.pentaho.di.core.row.value.ValueMetaFactory;
import org.pentaho.di.core.variables.VariableSpace;
import org.pentaho.di.core.xml.XMLHandler;
import org.pentaho.di.i18n.BaseMessages;
import org.pentaho.di.repository.ObjectId;
import org.pentaho.di.repository.Repository;
import org.pentaho.di.trans.Trans;
import org.pentaho.di.trans.TransMeta;
import org.pentaho.di.trans.step.BaseStepMeta;
import org.pentaho.di.trans.step.StepDataInterface;
import org.pentaho.di.trans.step.StepInterface;
import org.pentaho.di.trans.step.StepMeta;
import org.pentaho.di.trans.step.StepMetaInterface;
import org.pentaho.di.trans.steps.solrinput.SolrInput;
import org.pentaho.di.trans.steps.solrinput.SolrInputData;
import org.pentaho.di.trans.steps.solrinput.SolrInputField;
import org.pentaho.di.core.exception.KettleStepException;
import org.pentaho.metastore.api.IMetaStore;
import org.w3c.dom.Node;

@SuppressWarnings("deprecation")
@Step(id = "SolrInputStep", i18nPackageName = "org.pentaho.di.trans.steps.solrinput", name = "SolrInput.TypeLongDesc.SolrInput", description = "SolrInput.TypeTooltipDesc.SolrInput", categoryDescription = "i18n:org.pentaho.di.trans.step:BaseStep.Category.Input", image = "SolrInput.svg", documentationUrl = "http://wiki.pentaho.com")
public class SolrInputMeta extends BaseStepMeta implements StepMetaInterface {

  private static Class<?> PKG = SolrInputMeta.class; // for i18n purposes

  public static final String FIELD_TYPE_ELEMENT = "Element";
  public static final String FIELD_TYPE_METRIC = "Metric";
  
  private String URL;
  private String q;
  private String fl;
  private String fq;
  private String rows;
  private String facetField;
  private String facetQuery;
  
  /** The fields to return... */
  private SolrInputField[] inputFields;
  private int nrFields;

  public SolrInputMeta() {
    super();
  }
  
  /**
   * @return Returns the input fields.
   */
  public SolrInputField[] getInputFields() {
    return inputFields;
  }

  /**
   * @param inputFields
   *          The input fields to set.
   */
  public void setInputFields( SolrInputField[] inputFields ) {
    this.inputFields = inputFields;
  }

  public String getURL() {
	return URL;
  }

  public void setURL( String URL ) {
    this.URL = URL;
  }
  
  public String getQ() {
	return q;
  }

  public void setQ( String q ) {
    this.q = q;
  }
  
  public String getFq() {
	return fq;
  }

  public void setFq( String fq ) {
    this.fq = fq;
  }
  
  public String getFl() {
	return fl;
  }

  public void setFl( String fl ) {
    this.fl = fl;
  }
  
  public String getRows() {
	return rows;
  }

  public void setRows( String rows ) {
    this.rows = rows;
  }
  
  public String getFacetField() {
	return facetField;
  }

  public void setFacetField( String facetField ) {
    this.facetField = facetField;
  }
  
  public String getFacetQuery() {
	return facetQuery;
  }

  public void setFacetQuery( String facetQuery ) {
    this.facetQuery = facetQuery;
  }
  
  public void allocate( int nrfields ) {
    inputFields = new SolrInputField[nrfields];
  }

  public int getNrFields() {
    return nrFields;
  }

  // set sensible defaults for a new step
  public void setDefault() {
    URL = "";
    q = "";
    fl = "";
    fq = "";
    rows = "";
    facetField = "";
    facetQuery = "";
    allocate( 0 );
  }

  public void getFields( RowMetaInterface r, String name, RowMetaInterface[] info, 
		                 StepMeta nextStep, VariableSpace space, 
		                 Repository repository, IMetaStore metaStore ) 
		                		 throws KettleStepException {
	    r.clear();
	    for ( int i = 0; i < inputFields.length; i++ ) {
	      SolrInputField field = inputFields[i];
	      int type = field.getType();
	      if ( type == ValueMetaBase.TYPE_NONE ) {
	        type = ValueMetaBase.TYPE_STRING;
	      }
	      try {
	        ValueMetaInterface v = ValueMetaFactory.createValueMeta( space.environmentSubstitute( field.getName() ), type );
	        v.setOrigin( name );
	        v.setLength( field.getLength() );
	        v.setConversionMask( field.getFormat() );
	        v.setPrecision( field.getPrecision() );
	        v.setDecimalSymbol( field.getDecimalSymbol() );
	        v.setGroupingSymbol( field.getGroupSymbol() );
	        v.setCurrencySymbol( field.getCurrencySymbol() );
	        r.addValueMeta( v );
	      } catch ( Exception e ) {
	        throw new KettleStepException( e );
	      }
	    }
  }
  
  public void loadXML( Node stepnode, List<DatabaseMeta> databases, IMetaStore metaStore ) throws KettleXMLException {
	  readData( stepnode );
  }

  public Object clone() {
    SolrInputMeta retval = (SolrInputMeta) super.clone();

    int nrFields = inputFields.length;

    retval.allocate( nrFields );

    for ( int i = 0; i < nrFields; i++ ) {
      if ( inputFields[i] != null ) {
        retval.inputFields[i] = (SolrInputField) inputFields[i].clone();
      }
    }

    return retval;
  }

  public String getXML() throws KettleValueException {

    StringBuilder retval = new StringBuilder( 800 );
    retval.append( "    " ).append( XMLHandler.addTagValue( "URL", URL ) );
    retval.append( "    " ).append( XMLHandler.addTagValue( "q", q ) );
    retval.append( "    " ).append( XMLHandler.addTagValue( "fq", fq ) );
    retval.append( "    " ).append( XMLHandler.addTagValue( "fl", fl ) );
    retval.append( "    " ).append( XMLHandler.addTagValue( "rows", rows ) );
    retval.append( "    " ).append( XMLHandler.addTagValue( "facetField", facetField ) );
    retval.append( "    " ).append( XMLHandler.addTagValue( "facetQuery", facetQuery ) );
    retval.append( "    <fields>" + Const.CR );
    for ( int i = 0; i < inputFields.length; i++ ) {
      SolrInputField field = inputFields[i];
      retval.append( field.getXML() );
    }
    retval.append( "      </fields>" + Const.CR );
    return retval.toString();
  }

  private void readData( Node stepnode ) throws KettleXMLException {
	    try {
	      URL = XMLHandler.getTagValue( stepnode, "URL" );
	      q = XMLHandler.getTagValue( stepnode, "q" );
	      fq = XMLHandler.getTagValue( stepnode, "fq" );
	      fl = XMLHandler.getTagValue( stepnode, "fl" );
	      rows = XMLHandler.getTagValue( stepnode, "rows" );
	      facetField = XMLHandler.getTagValue( stepnode, "facetField" );
	      facetQuery = XMLHandler.getTagValue( stepnode, "facetQuery" );

	      Node fields = XMLHandler.getSubNode( stepnode, "fields" );
	      int nrFields = XMLHandler.countNodes( fields, "field" );

	      allocate( nrFields );

	      for ( int i = 0; i < nrFields; i++ ) {
	        Node fnode = XMLHandler.getSubNodeByNr( fields, "field", i );
	        SolrInputField field = new SolrInputField( fnode );
	        inputFields[i] = field;
	      }
	      
	    } catch ( Exception e ) {
	      throw new KettleXMLException( "Unable to load step info from XML", e );
	    }
	  }
  
  public void readRep( Repository rep, IMetaStore metaStore, ObjectId id_step, List<DatabaseMeta> databases ) throws KettleException {
	    try {
	      URL = rep.getStepAttributeString( id_step, "URL" );
	      q = rep.getStepAttributeString( id_step, "q" );
	      fq = rep.getStepAttributeString( id_step, "fq" );
	      fl = rep.getStepAttributeString( id_step, "fl" );
	      rows = rep.getStepAttributeString( id_step,  "rows" );
	      facetField = rep.getStepAttributeString( id_step, "facetField" );
	      facetQuery = rep.getStepAttributeString( id_step, "facetQuery" );

	      int nrFields = rep.countNrStepAttributes( id_step, "field_name" );

	      allocate( nrFields );

	      for ( int i = 0; i < nrFields; i++ ) {
	        SolrInputField field = new SolrInputField();

	        field.setName( rep.getStepAttributeString( id_step, i, "field_name" ) );
	        field.setType( ValueMeta.getType( rep.getStepAttributeString( id_step, i, "field_type" ) ) );
	        field.setFormat( rep.getStepAttributeString( id_step, i, "field_format" ) );
	        field.setCurrencySymbol( rep.getStepAttributeString( id_step, i, "field_currency" ) );
	        field.setDecimalSymbol( rep.getStepAttributeString( id_step, i, "field_decimal" ) );
	        field.setGroupSymbol( rep.getStepAttributeString( id_step, i, "field_group" ) );
	        field.setLength( (int) rep.getStepAttributeInteger( id_step, i, "field_length" ) );
	        field.setPrecision( (int) rep.getStepAttributeInteger( id_step, i, "field_precision" ) );
	        field.setTrimType( SolrInputField.getTrimTypeByCode( rep.getStepAttributeString(
	          id_step, i, "field_trim_type" ) ) );
	        inputFields[i] = field;
	      }
	    } catch ( Exception e ) {
	      throw new KettleException( BaseMessages.getString(
	        PKG, "SolrInputMeta.Exception.ErrorReadingRepository" ), e );
	    }
	  }

	  public void saveRep( Repository rep, IMetaStore metaStore, ObjectId id_transformation, ObjectId id_step ) throws KettleException {
	    try {
	      rep.saveStepAttribute( id_transformation, id_step, "URL", URL );
	      rep.saveStepAttribute( id_transformation, id_step, "q", q );
	      rep.saveStepAttribute( id_transformation, id_step, "fq", fq );
	      rep.saveStepAttribute( id_transformation, id_step, "fl", fl );
	      rep.saveStepAttribute( id_transformation, id_step, "rows", rows );
	      rep.saveStepAttribute( id_transformation, id_step, "facetField", facetField );
	      rep.saveStepAttribute( id_transformation, id_step, "facetQuery", facetQuery );
	      
	      for ( int i = 0; i < inputFields.length; i++ ) {
	        SolrInputField field = inputFields[i];
	        rep.saveStepAttribute( id_transformation, id_step, i, "field_name", field.getName() );
	        rep.saveStepAttribute( id_transformation, id_step, i, "field_type", field.getTypeDesc() );
	        rep.saveStepAttribute( id_transformation, id_step, i, "field_format", field.getFormat() );
	        rep.saveStepAttribute( id_transformation, id_step, i, "field_currency", field.getCurrencySymbol() );
	        rep.saveStepAttribute( id_transformation, id_step, i, "field_decimal", field.getDecimalSymbol() );
	        rep.saveStepAttribute( id_transformation, id_step, i, "field_group", field.getGroupSymbol() );
	        rep.saveStepAttribute( id_transformation, id_step, i, "field_length", field.getLength() );
	        rep.saveStepAttribute( id_transformation, id_step, i, "field_precision", field.getPrecision() );
	        rep.saveStepAttribute( id_transformation, id_step, i, "field_trim_type", field.getTrimTypeCode() );
	      }
	    } catch ( Exception e ) {
	      throw new KettleException( BaseMessages.getString(
	        PKG, "SolrInputMeta.Exception.ErrorSavingToRepository", "" + id_step ), e );
	    }
	  }

	  public void check( List<CheckResultInterface> remarks, TransMeta transMeta, StepMeta stepMeta,
	    RowMetaInterface prev, String[] input, String[] output, RowMetaInterface info, VariableSpace space,
	    Repository repository, IMetaStore metaStore ) {
	    CheckResult cr;

	    // See if we get input...
	    if ( input.length > 0 ) {
	      cr =
	        new CheckResult( CheckResult.TYPE_RESULT_ERROR, BaseMessages.getString(
	          PKG, "SolrInputMeta.CheckResult.NoInputExpected" ), stepMeta );
	    } else {
	      cr =
	        new CheckResult( CheckResult.TYPE_RESULT_OK, BaseMessages.getString(
	          PKG, "SolrInputMeta.CheckResult.NoInput" ), stepMeta );
	    }
	    remarks.add( cr );

	    // check URL
	    if ( Const.isEmpty( URL ) ) {
	      cr =
	        new CheckResult( CheckResult.TYPE_RESULT_ERROR, BaseMessages.getString(
	          PKG, "SolrInputMeta.CheckResult.NoURL" ), stepMeta );
	    } else {
	      cr =
	        new CheckResult( CheckResult.TYPE_RESULT_OK, BaseMessages.getString(
	          PKG, "SolrInputMeta.CheckResult.URLOk" ), stepMeta );
	    }
	    remarks.add( cr );
	    
	    // check return fields
	    if ( inputFields.length == 0 ) {
	      cr =
	        new CheckResult( CheckResult.TYPE_RESULT_ERROR, BaseMessages.getString(
	          PKG, "SolrInputMeta.CheckResult.NoFields" ), stepMeta );
	    } else {
	      cr =
	        new CheckResult( CheckResult.TYPE_RESULT_OK, BaseMessages.getString(
	          PKG, "SolrInputMeta.CheckResult.FieldsOk" ), stepMeta );
	    }
	    remarks.add( cr );
	  }

	  public StepInterface getStep( StepMeta stepMeta, StepDataInterface stepDataInterface, int cnr,
	    TransMeta transMeta, Trans trans ) {
	    return new SolrInput( stepMeta, stepDataInterface, cnr, transMeta, trans );
	  }

	  public StepDataInterface getStepData() {
	    return new SolrInputData();
	  }
	  
}
