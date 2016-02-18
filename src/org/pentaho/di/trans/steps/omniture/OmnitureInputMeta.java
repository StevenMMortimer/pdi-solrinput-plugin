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

package org.pentaho.di.trans.steps.omniture;
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
import org.pentaho.di.trans.steps.omniture.OmnitureInput;
import org.pentaho.di.trans.steps.omniture.OmnitureInputData;
import org.pentaho.di.trans.steps.omniture.OmnitureInputField;
import org.pentaho.di.core.exception.KettleStepException;
import org.pentaho.metastore.api.IMetaStore;
import org.w3c.dom.Node;

@SuppressWarnings("deprecation")
@Step(id = "OmnitureInputStep", i18nPackageName = "org.pentaho.di.trans.steps.omniture", name = "OmnitureInput.TypeLongDesc.OmnitureInput", description = "OmnitureInput.TypeTooltipDesc.OmnitureInput", categoryDescription = "i18n:org.pentaho.di.trans.step:BaseStep.Category.Input", image = "OmnitureInput.svg", documentationUrl = "http://wiki.pentaho.com")
public class OmnitureInputMeta extends BaseStepMeta implements StepMetaInterface {

  private static Class<?> PKG = OmnitureInputMeta.class; // for i18n purposes

  public static final String FIELD_TYPE_ELEMENT = "Element";
  public static final String FIELD_TYPE_METRIC = "Metric";
  
  private String userName;
  private String secret;
  private String reportSuiteId;
  private String startDate;
  private String endDate;
  private String dateGranularity;
  private String elements;
  private String metrics;
  private String segments;
  /** The fields to return... */
  private OmnitureInputField[] inputFields;
  private int nrFields;

  public OmnitureInputMeta() {
    super();
  }
  
  /**
   * @return Returns the input fields.
   */
  public OmnitureInputField[] getInputFields() {
    return inputFields;
  }

  /**
   * @param inputFields
   *          The input fields to set.
   */
  public void setInputFields( OmnitureInputField[] inputFields ) {
    this.inputFields = inputFields;
  }

  public String getUserName() {
	return userName;
  }

  public void setUserName( String userName ) {
    this.userName = userName;
  }
  
  public String getSecret() {
	return secret;
  }

  public void setSecret( String secret ) {
    this.secret = secret;
  }
  
  public String getReportSuiteId() {
	return reportSuiteId;
  }

  public void setReportSuiteId( String reportSuiteId ) {
    this.reportSuiteId = reportSuiteId;
  }

  public String getSegments() {
    return segments;
  }

  public void setSegments( String segments ) {
    this.segments = segments;
  }

  public String getElements() {
    return elements;
  }

  public void setElements( String elements ) {
    this.elements = elements;
  }

  public String getMetrics() {
    return metrics;
  }

  public void setMetrics( String metrics ) {
    this.metrics = metrics;
  }

  public String getStartDate() {
    return startDate;
  }

  public void setStartDate( String startDate ) {
    this.startDate = startDate;
  }

  public String getEndDate() {
    return endDate;
  }

  public void setEndDate( String endDate ) {
    this.endDate = endDate;
  }
  
  public String getDateGranularity() {
    return dateGranularity;
  }

  public void setDateGranularity( String dateGranularity ) {
    this.dateGranularity = dateGranularity;
  }
  
  public void allocate( int nrfields ) {
    inputFields = new OmnitureInputField[nrfields];
  }

  public int getNrFields() {
    return nrFields;
  }

  // set sensible defaults for a new step
  public void setDefault() {
    userName = "username:Company";
    secret = ""; //"123abc456def789ghi012jkl345";
    reportSuiteId = "Your Report Suite Id";
    elements = "page";
    metrics = "visits";
    startDate = new SimpleDateFormat( "yyyy-MM-dd" ).format( new Date() );
    endDate = new String( startDate );
    dateGranularity = "DAY";
    segments = "";
    allocate( 0 );
  }

  public void getFields( RowMetaInterface r, String name, RowMetaInterface[] info, 
		                 StepMeta nextStep, VariableSpace space, 
		                 Repository repository, IMetaStore metaStore ) 
		                		 throws KettleStepException {
	    r.clear();
	    for ( int i = 0; i < inputFields.length; i++ ) {
	      OmnitureInputField field = inputFields[i];
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
    OmnitureInputMeta retval = (OmnitureInputMeta) super.clone();

    int nrFields = inputFields.length;

    retval.allocate( nrFields );

    for ( int i = 0; i < nrFields; i++ ) {
      if ( inputFields[i] != null ) {
        retval.inputFields[i] = (OmnitureInputField) inputFields[i].clone();
      }
    }

    return retval;
  }

  public String getXML() throws KettleValueException {

    StringBuilder retval = new StringBuilder( 800 );
    retval.append( "    " ).append( XMLHandler.addTagValue( "userName", userName ) );
    retval.append( "    " ).append( XMLHandler.addTagValue( "secret", secret ) );
    retval.append( "    " ).append( XMLHandler.addTagValue( "reportSuiteId", reportSuiteId ) );
    retval.append( "    " ).append( XMLHandler.addTagValue( "startDate", startDate ) );
    retval.append( "    " ).append( XMLHandler.addTagValue( "endDate", endDate ) );
    retval.append( "    " ).append( XMLHandler.addTagValue( "dateGranularity", dateGranularity ) );
    retval.append( "    " ).append( XMLHandler.addTagValue( "elements", elements ) );
    retval.append( "    " ).append( XMLHandler.addTagValue( "metrics", metrics ) );
    retval.append( "    " ).append( XMLHandler.addTagValue( "segments", segments ) );
    retval.append( "    <fields>" + Const.CR );
    for ( int i = 0; i < inputFields.length; i++ ) {
      OmnitureInputField field = inputFields[i];
      retval.append( field.getXML() );
    }
    retval.append( "      </fields>" + Const.CR );
    return retval.toString();
  }

  private void readData( Node stepnode ) throws KettleXMLException {
	    try {
	      userName = XMLHandler.getTagValue( stepnode, "userName" );
	      secret = XMLHandler.getTagValue( stepnode, "secret" );
	      if ( secret != null && secret.startsWith( "Encrypted" ) ) {
	    	  secret = Encr.decryptPassword( secret.replace( "Encrypted", "" ).replace( " ", "" ) );
	      }
	      reportSuiteId = XMLHandler.getTagValue( stepnode, "reportSuiteId" );
	      startDate = XMLHandler.getTagValue( stepnode, "startDate" );
	      endDate = XMLHandler.getTagValue( stepnode, "endDate" );
	      dateGranularity = XMLHandler.getTagValue( stepnode, "dateGranularity" );
	      elements = XMLHandler.getTagValue( stepnode, "elements" );
	      metrics = XMLHandler.getTagValue( stepnode, "metrics" );
	      segments = XMLHandler.getTagValue( stepnode, "segments" );

	      Node fields = XMLHandler.getSubNode( stepnode, "fields" );
	      int nrFields = XMLHandler.countNodes( fields, "field" );

	      allocate( nrFields );

	      for ( int i = 0; i < nrFields; i++ ) {
	        Node fnode = XMLHandler.getSubNodeByNr( fields, "field", i );
	        OmnitureInputField field = new OmnitureInputField( fnode );
	        inputFields[i] = field;
	      }
	      
	    } catch ( Exception e ) {
	      throw new KettleXMLException( "Unable to load step info from XML", e );
	    }
	  }
  
  public void readRep( Repository rep, IMetaStore metaStore, ObjectId id_step, List<DatabaseMeta> databases ) throws KettleException {
	    try {
	      userName = rep.getStepAttributeString( id_step, "userName" );
	      secret = rep.getStepAttributeString( id_step, "secret" );
	      if ( secret != null && secret.startsWith( "Encrypted" ) ) {
	    	  secret = Encr.decryptPassword( secret.replace( "Encrypted", "" ).replace( " ", "" ) );
	      }
	      reportSuiteId = rep.getStepAttributeString( id_step, "reportSuiteId" );
	      startDate = rep.getStepAttributeString( id_step, "startDate" );
	      endDate = rep.getStepAttributeString( id_step, "endDate" );
	      dateGranularity = rep.getStepAttributeString( id_step,  "dateGranularity" );
	      elements = rep.getStepAttributeString( id_step, "elements" );
	      metrics = rep.getStepAttributeString( id_step, "metrics" );
	      segments = rep.getStepAttributeString( id_step, "segments" );

	      int nrFields = rep.countNrStepAttributes( id_step, "field_name" );

	      allocate( nrFields );

	      for ( int i = 0; i < nrFields; i++ ) {
	        OmnitureInputField field = new OmnitureInputField();

	        field.setName( rep.getStepAttributeString( id_step, i, "field_name" ) );
	        field.setType( ValueMeta.getType( rep.getStepAttributeString( id_step, i, "field_type" ) ) );
	        field.setFormat( rep.getStepAttributeString( id_step, i, "field_format" ) );
	        field.setCurrencySymbol( rep.getStepAttributeString( id_step, i, "field_currency" ) );
	        field.setDecimalSymbol( rep.getStepAttributeString( id_step, i, "field_decimal" ) );
	        field.setGroupSymbol( rep.getStepAttributeString( id_step, i, "field_group" ) );
	        field.setLength( (int) rep.getStepAttributeInteger( id_step, i, "field_length" ) );
	        field.setPrecision( (int) rep.getStepAttributeInteger( id_step, i, "field_precision" ) );
	        field.setTrimType( OmnitureInputField.getTrimTypeByCode( rep.getStepAttributeString(
	          id_step, i, "field_trim_type" ) ) );
	        inputFields[i] = field;
	      }
	    } catch ( Exception e ) {
	      throw new KettleException( BaseMessages.getString(
	        PKG, "OmnitureInputMeta.Exception.ErrorReadingRepository" ), e );
	    }
	  }

	  public void saveRep( Repository rep, IMetaStore metaStore, ObjectId id_transformation, ObjectId id_step ) throws KettleException {
	    try {
	      rep.saveStepAttribute( id_transformation, id_step, "userName", userName );
	      rep.saveStepAttribute( id_transformation, id_step, "secret", Encr.encryptPasswordIfNotUsingVariables( secret ) );
	      rep.saveStepAttribute( id_transformation, id_step, "reportSuiteId", reportSuiteId );
	      rep.saveStepAttribute( id_transformation, id_step, "startDate", startDate );
	      rep.saveStepAttribute( id_transformation, id_step, "endDate", endDate );
	      rep.saveStepAttribute( id_transformation, id_step, "dateGranularity", dateGranularity );
	      rep.saveStepAttribute( id_transformation, id_step, "elements", elements );
	      rep.saveStepAttribute( id_transformation, id_step, "metrics", metrics );
	      rep.saveStepAttribute( id_transformation, id_step, "segments", segments );

	      for ( int i = 0; i < inputFields.length; i++ ) {
	        OmnitureInputField field = inputFields[i];
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
	        PKG, "OmnitureInputMeta.Exception.ErrorSavingToRepository", "" + id_step ), e );
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
	          PKG, "OmnitureInputMeta.CheckResult.NoInputExpected" ), stepMeta );
	    } else {
	      cr =
	        new CheckResult( CheckResult.TYPE_RESULT_OK, BaseMessages.getString(
	          PKG, "OmnitureInputMeta.CheckResult.NoInput" ), stepMeta );
	    }
	    remarks.add( cr );

	    // check userName
	    if ( Const.isEmpty( userName ) ) {
	      cr =
	        new CheckResult( CheckResult.TYPE_RESULT_ERROR, BaseMessages.getString(
	          PKG, "OmnitureInputMeta.CheckResult.NoUsername" ), stepMeta );
	    } else {
	      cr =
	        new CheckResult( CheckResult.TYPE_RESULT_OK, BaseMessages.getString(
	          PKG, "OmnitureInputMeta.CheckResult.UsernameOk" ), stepMeta );
	    }
	    remarks.add( cr );
	    
	    // check secret
	    if ( Const.isEmpty( secret ) ) {
	      cr =
	        new CheckResult( CheckResult.TYPE_RESULT_ERROR, BaseMessages.getString(
	          PKG, "OmnitureInputMeta.CheckResult.NoSecret" ), stepMeta );
	    } else {
	      cr =
	        new CheckResult( CheckResult.TYPE_RESULT_OK, BaseMessages.getString(
	          PKG, "OmnitureInputMeta.CheckResult.SecretOk" ), stepMeta );
	    }
	    remarks.add( cr );
	    
	    // check reportSuiteId
	    if ( Const.isEmpty( reportSuiteId ) ) {
	      cr =
	        new CheckResult( CheckResult.TYPE_RESULT_ERROR, BaseMessages.getString(
	          PKG, "OmnitureInputMeta.CheckResult.NoReportSuiteId" ), stepMeta );
	    } else {
	      cr =
	        new CheckResult( CheckResult.TYPE_RESULT_OK, BaseMessages.getString(
	          PKG, "OmnitureInputMeta.CheckResult.ReportSuiteIdOk" ), stepMeta );
	    }
	    remarks.add( cr );

	    // check return fields
	    if ( inputFields.length == 0 ) {
	      cr =
	        new CheckResult( CheckResult.TYPE_RESULT_ERROR, BaseMessages.getString(
	          PKG, "OmnitureInputMeta.CheckResult.NoFields" ), stepMeta );
	    } else {
	      cr =
	        new CheckResult( CheckResult.TYPE_RESULT_OK, BaseMessages.getString(
	          PKG, "OmnitureInputMeta.CheckResult.FieldsOk" ), stepMeta );
	    }
	    remarks.add( cr );
	  }

	  public StepInterface getStep( StepMeta stepMeta, StepDataInterface stepDataInterface, int cnr,
	    TransMeta transMeta, Trans trans ) {
	    return new OmnitureInput( stepMeta, stepDataInterface, cnr, transMeta, trans );
	  }

	  public StepDataInterface getStepData() {
	    return new OmnitureInputData();
	  }
	  
}
