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

package org.pentaho.di.ui.trans.steps.omniture;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.adobe.analytics.client.*;
import com.adobe.analytics.client.domain.*;
import com.adobe.analytics.client.methods.*;
import org.eclipse.swt.SWT;
import org.pentaho.di.ui.core.dialog.EnterNumberDialog;
import org.pentaho.di.ui.core.dialog.EnterTextDialog;
import org.pentaho.di.ui.core.dialog.PreviewRowsDialog;
import org.pentaho.di.ui.trans.dialog.TransPreviewProgressDialog;
import org.pentaho.di.trans.Trans;
import org.pentaho.di.trans.TransMeta;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.ShellAdapter;
import org.eclipse.swt.events.ShellEvent;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.pentaho.di.ui.core.widget.ColumnInfo;
import org.pentaho.di.ui.core.dialog.ErrorDialog;
import org.pentaho.di.ui.core.widget.ComboVar;
import org.pentaho.di.core.Const;
import org.pentaho.di.core.Props;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.row.value.ValueMetaBase;
import org.pentaho.di.i18n.BaseMessages;
import org.pentaho.di.trans.TransPreviewFactory;
import org.pentaho.di.trans.step.BaseStepMeta;
import org.pentaho.di.trans.step.StepDialogInterface;
import org.pentaho.di.trans.steps.omniture.OmnitureInputField;
import org.pentaho.di.trans.steps.omniture.OmnitureInputMeta;
import org.pentaho.di.ui.core.widget.LabelTextVar;
import org.pentaho.di.ui.core.widget.TableView;
import org.pentaho.di.ui.core.widget.TextVar;
import org.pentaho.di.ui.trans.step.BaseStepDialog;

public class OmnitureInputDialog extends BaseStepDialog implements StepDialogInterface {

  private static Class<?> PKG = OmnitureInputMeta.class; // for i18n purposes

  private OmnitureInputMeta input;

  private CTabFolder wTabFolder;
  private Composite wSetupComp, wFieldsComp;
  private CTabItem wSetupTab, wFieldsTab;
  
  private FormData fdTabFolder, fdFieldsComp;
  private FormData fdFields;
  
  private Group wConnectGroup;
  private FormData fdConnectGroup;
  
  private Group wReportGroup;
  private FormData fdReportGroup;
  
  private LabelTextVar wUserName, wSecret;
  private FormData fdUserName, fdSecret;
  
  private ComboVar wReportSuiteId;
  private Label wlReportSuiteId;
  private FormData fdlReportSuiteId, fdReportSuiteId;
  private boolean gotReportSuiteIds = false;
  private boolean getReportSuiteIdsListError = false; /* True if error getting report suite id list */
  
  private Button wTest;
  private FormData fdTest;

  private TableView wFields;

  private Label wlQuStartDate;
  private TextVar wQuStartDate;

  private Label wlQuEndDate;
  private TextVar wQuEndDate;
  
  private Label wlDateGranularity;
  private CCombo wQuDateGranularity;

  private Label wlQuElements;
  private TextVar wQuElements;

  private Label wlQuMetrics;
  private TextVar wQuMetrics;
  
  private Label wlQuSegments;
  private TextVar wQuSegments;

  private Link wQuElementsReference;
  private Link wQuMetricsReference;
  private Link wQuSegmentsReference;

  private int middle;
  private int margin;

  private ColumnInfo[] colinf;

  private ModifyListener lsMod;
  
  public static final String[] dateGranularityOptions = { "", "SECONDS", "HOUR", "DAY", "WEEK", "MONTH", "QUARTER", "YEAR" };

  static final String REFERENCE_METRICS_URI =
    "https://marketing.adobe.com/developer/documentation/analytics-reporting-1-4/metrics";
  static final String REFERENCE_ELEMENTS_URI =
    "https://marketing.adobe.com/developer/documentation/analytics-reporting-1-4/elements";
  static final String REFERENCE_SEGMENTS_URI =
    "https://marketing.adobe.com/resources/help/en_US/analytics/segment";
  
  // constructor
  public OmnitureInputDialog( Shell parent, Object in, TransMeta transMeta, String sname ) {
    super( parent, (BaseStepMeta) in, transMeta, sname );
    setInput( (OmnitureInputMeta) in );
  }

  // builds and shows the dialog
public String open() {
    Shell parent = getParent();
    Display display = parent.getDisplay();

    shell = new Shell( parent, SWT.DIALOG_TRIM | SWT.RESIZE | SWT.MIN | SWT.MAX );
    props.setLook( shell );
    setShellImage( shell, getInput() );

    lsMod = new ModifyListener() {
      public void modifyText( ModifyEvent e ) {
        getInput().setChanged();
      }
    };
    backupChanged = getInput().hasChanged();

    FormLayout formLayout = new FormLayout();
    formLayout.marginWidth = Const.FORM_MARGIN;
    formLayout.marginHeight = Const.FORM_MARGIN;

    shell.setLayout( formLayout );
    shell.setText( BaseMessages.getString( PKG, "OmnitureInputDialog.Shell.Title" ) );

    middle = props.getMiddlePct();
    margin = Const.MARGIN;
    
    
    /*************************************************
     * // STEP NAME ENTRY
     *************************************************/

    // Stepname line
    wlStepname = new Label( shell, SWT.RIGHT );
    wlStepname.setText( BaseMessages.getString( PKG, "System.Label.StepName" ) );
    props.setLook( wlStepname );
    fdlStepname = new FormData();
    fdlStepname.left = new FormAttachment( 0, 0 );
    fdlStepname.right = new FormAttachment( middle, -margin );
    fdlStepname.top = new FormAttachment( 0, margin );
    wlStepname.setLayoutData( fdlStepname );

    wStepname = new Text( shell, SWT.SINGLE | SWT.LEFT | SWT.BORDER );
    wStepname.setText( stepname );
    props.setLook( wStepname );
    wStepname.addModifyListener( lsMod );
    fdStepname = new FormData();
    fdStepname.left = new FormAttachment( middle, 0 );
    fdStepname.top = new FormAttachment( 0, margin );
    fdStepname.right = new FormAttachment( 100, 0 );
    wStepname.setLayoutData( fdStepname );
    
    wTabFolder = new CTabFolder( shell, SWT.BORDER );
    props.setLook( wTabFolder, Props.WIDGET_STYLE_TAB );

    // ////////////////////////
    // START OF GENERAL TAB ///
    // ////////////////////////

    wSetupTab = new CTabItem( wTabFolder, SWT.NONE );
    wSetupTab.setText( BaseMessages.getString( PKG, "OmnitureInputDialog.Tab.Setup.Label" ) );
    wSetupComp = new Composite( wTabFolder, SWT.NONE );
    props.setLook( wSetupComp );
    FormLayout generalLayout = new FormLayout();
    generalLayout.marginWidth = 3;
    generalLayout.marginHeight = 3;
    wSetupComp.setLayout( generalLayout );
    wSetupTab.setControl( wSetupComp );
    
    /*************************************************
     * // OMNITURE CONNECTION GROUP
     *************************************************/
    wConnectGroup = new Group( wSetupComp, SWT.SHADOW_ETCHED_IN );
    wConnectGroup.setText( BaseMessages.getString( PKG, "OmnitureInputDialog.ConnectGroup.Label" ) );
    FormLayout fconnLayout = new FormLayout();
    fconnLayout.marginWidth = 3;
    fconnLayout.marginHeight = 3;
    wConnectGroup.setLayout( fconnLayout );
    props.setLook( wConnectGroup );

    // UserName line
    wUserName = new LabelTextVar( transMeta, wConnectGroup,
      BaseMessages.getString( PKG, "OmnitureInputDialog.User.Label" ),
      BaseMessages.getString( PKG, "OmnitureInputDialog.User.Tooltip" ) );
    props.setLook( wUserName );
    wUserName.addModifyListener( lsMod );
    fdUserName = new FormData();
    fdUserName.left = new FormAttachment( 0, 0 );
    fdUserName.top = new FormAttachment( 0, margin );
    fdUserName.right = new FormAttachment( 100, 0 );
    wUserName.setLayoutData( fdUserName );

    // Secret line
    wSecret = new LabelTextVar( transMeta, wConnectGroup,
      BaseMessages.getString( PKG, "OmnitureInputDialog.Secret.Label" ),
      BaseMessages.getString( PKG, "OmnitureInputDialog.Secret.Tooltip" ), true );
    props.setLook( wSecret );
    wSecret.addModifyListener( lsMod );
    fdSecret = new FormData();
    fdSecret.left = new FormAttachment( 0, 0 );
    fdSecret.top = new FormAttachment( wUserName, margin );
    fdSecret.right = new FormAttachment( 100, 0 );
    wSecret.setLayoutData( fdSecret );
    
    // ReportSuiteId line
    wlReportSuiteId = new Label( wConnectGroup, SWT.RIGHT );
    wlReportSuiteId.setText( BaseMessages.getString( PKG, "OmnitureInputDialog.ReportSuiteId.Label" ) );
    props.setLook( wlReportSuiteId );
    fdlReportSuiteId = new FormData();
    fdlReportSuiteId.top = new FormAttachment( wSecret, margin );
    fdlReportSuiteId.left = new FormAttachment( 0, 0 );
    fdlReportSuiteId.right = new FormAttachment( middle, -margin );
    wlReportSuiteId.setLayoutData( fdlReportSuiteId );
    wReportSuiteId = new ComboVar( transMeta, wConnectGroup, SWT.BORDER | SWT.READ_ONLY );
    wReportSuiteId.setEditable( true );
    props.setLook( wReportSuiteId );
    wReportSuiteId.addModifyListener( lsMod );
    fdReportSuiteId = new FormData();
    fdReportSuiteId.top = new FormAttachment( wSecret, margin );
    fdReportSuiteId.left = new FormAttachment( middle, 0 );
    fdReportSuiteId.right = new FormAttachment( 100, 0 );
    wReportSuiteId.setLayoutData( fdReportSuiteId );
    wReportSuiteId.addFocusListener( new FocusListener() {
      public void focusLost( org.eclipse.swt.events.FocusEvent e ) {
    	  getReportSuiteIdsListError = false;
      }
      public void focusGained( org.eclipse.swt.events.FocusEvent e ) {
        // check if the login credentials passed and not just had error
        if ( Const.isEmpty( wUserName.getText() ) || Const.isEmpty( wSecret.getText() )
          || ( getReportSuiteIdsListError ) ) {
          return;
        }
        Cursor busy = new Cursor( shell.getDisplay(), SWT.CURSOR_WAIT );
        shell.setCursor( busy );
        getReportSuiteIdsList();
        shell.setCursor( null );
        busy.dispose();
      }
    } );
    
    // Test Omniture connection button
    wTest = new Button( wConnectGroup, SWT.PUSH );
    wTest.setText( BaseMessages.getString( PKG, "OmnitureInputDialog.TestConnection.Label" ) );
    props.setLook( wTest );
    fdTest = new FormData();
    wTest.setToolTipText( BaseMessages.getString( PKG, "OmnitureInputDialog.TestConnection.Tooltip" ) );
    // fdTest.left = new FormAttachment(middle, 0);
    fdTest.top = new FormAttachment( wReportSuiteId, margin );
    fdTest.right = new FormAttachment( 100, 0 );
    wTest.setLayoutData( fdTest );
    
    wTest.addListener( SWT.Selection, new Listener() {
        @Override
        public void handleEvent( Event e ) {
	        Cursor busy = new Cursor( shell.getDisplay(), SWT.CURSOR_WAIT );
	        shell.setCursor( busy );
	        testConnection();
	        shell.setCursor( null );
	        busy.dispose();
        }
      }
    );

    fdConnectGroup = new FormData();
    fdConnectGroup.left = new FormAttachment( 0, 0 );
    fdConnectGroup.right = new FormAttachment( 100, 0 );
    fdConnectGroup.top = new FormAttachment( 0, margin );
    wConnectGroup.setLayoutData( fdConnectGroup );

    /*************************************************
     * // OMNITURE REPORT DEFINITION
     *************************************************/
    
    wReportGroup = new Group( wSetupComp, SWT.SHADOW_ETCHED_IN );
    wReportGroup.setText( BaseMessages.getString( PKG, "OmnitureInputDialog.ReportGroup.Label" ) );
    FormLayout freportLayout = new FormLayout();
    freportLayout.marginWidth = 3;
    freportLayout.marginHeight = 3;
    wReportGroup.setLayout( freportLayout );
    props.setLook( wReportGroup );

    // Report start date
    wlQuStartDate = new Label( wReportGroup, SWT.RIGHT );
    wlQuStartDate.setText( BaseMessages.getString( PKG, "OmnitureInputDialog.Report.StartDate.Label" ) );
    props.setLook( wlQuStartDate );
    FormData fdlQuStartDate = new FormData();
    fdlQuStartDate.top = new FormAttachment( wConnectGroup, 2 * margin );
    fdlQuStartDate.left = new FormAttachment( 0, 0 );
    fdlQuStartDate.right = new FormAttachment( middle, -margin );
    wlQuStartDate.setLayoutData( fdlQuStartDate );
    
    wQuStartDate = new TextVar( transMeta, wReportGroup, SWT.SINGLE | SWT.LEFT | SWT.BORDER );
    wQuStartDate.addModifyListener( lsMod );
    wQuStartDate.setToolTipText( BaseMessages.getString( PKG, "OmnitureInputDialog.Report.StartDate.Tooltip" ) );
    props.setLook( wQuStartDate );
    FormData fdQuStartDate = new FormData();
    fdlQuStartDate.top = new FormAttachment( wConnectGroup, 2 * margin );
    fdQuStartDate.left = new FormAttachment( middle, 0 );
    fdQuStartDate.right = new FormAttachment( 100, 0 );
    wQuStartDate.setLayoutData( fdQuStartDate );

    // Report end date
    wlQuEndDate = new Label( wReportGroup, SWT.RIGHT );
    wlQuEndDate.setText( BaseMessages.getString( PKG, "OmnitureInputDialog.Report.EndDate.Label" ) );
    props.setLook( wlQuEndDate );
    FormData fdlQuEndDate = new FormData();
    fdlQuEndDate.top = new FormAttachment( wQuStartDate, margin );
    fdlQuEndDate.left = new FormAttachment( 0, 0 );
    fdlQuEndDate.right = new FormAttachment( middle, -margin );
    wlQuEndDate.setLayoutData( fdlQuEndDate );
    wQuEndDate = new TextVar( transMeta, wReportGroup, SWT.SINGLE | SWT.LEFT | SWT.BORDER );
    wQuEndDate.addModifyListener( lsMod );
    wQuEndDate.setToolTipText( BaseMessages.getString( PKG, "OmnitureInputDialog.Report.EndDate.Tooltip" ) );
    props.setLook( wQuEndDate );
    FormData fdQuEndDate = new FormData();
    fdQuEndDate.top = new FormAttachment( wQuStartDate, margin );
    fdQuEndDate.left = new FormAttachment( middle, 0 );
    fdQuEndDate.right = new FormAttachment( 100, 0 );
    wQuEndDate.setLayoutData( fdQuEndDate );
    
    // Report date granularity
    wlDateGranularity = new Label( wReportGroup, SWT.RIGHT );
    wlDateGranularity.setText( BaseMessages.getString( PKG, "OmnitureInputDialog.DateGranularity.Label" ) );
    props.setLook( wlDateGranularity );
    FormData fdlDateGranularity = new FormData();
    fdlDateGranularity.top = new FormAttachment( wQuEndDate, margin );
    fdlDateGranularity.left = new FormAttachment( 0, 0 );
    fdlDateGranularity.right = new FormAttachment( middle, -margin );
    wlDateGranularity.setLayoutData( fdlDateGranularity );
    wQuDateGranularity = new CCombo( wReportGroup, SWT.BORDER | SWT.READ_ONLY );
    props.setLook( wQuDateGranularity );
    wQuDateGranularity.addModifyListener( lsMod );
    FormData fdDateGranularity = new FormData();
    fdDateGranularity.top = new FormAttachment( wQuEndDate, margin );
    fdDateGranularity.left = new FormAttachment( middle, 0 );
    fdDateGranularity.right = new FormAttachment( 100, 0 );
    wQuDateGranularity.setLayoutData( fdDateGranularity );
    wQuDateGranularity.setItems( dateGranularityOptions );

    // Report elements
    wlQuElements = new Label( wReportGroup, SWT.RIGHT );
    wlQuElements.setText( BaseMessages.getString( PKG, "OmnitureInputDialog.Report.Elements.Label" ) );
    props.setLook( wlQuElements );
    FormData fdlQuElements = new FormData();
    fdlQuElements.top = new FormAttachment( wQuDateGranularity, margin );
    fdlQuElements.left = new FormAttachment( 0, 0 );
    fdlQuElements.right = new FormAttachment( middle, -margin );
    wlQuElements.setLayoutData( fdlQuElements );
    wQuElements = new TextVar( transMeta, wReportGroup, SWT.SINGLE | SWT.LEFT | SWT.BORDER );
    wQuElements.addModifyListener( lsMod );
    wQuElements.setToolTipText( BaseMessages.getString( PKG, "OmnitureInputDialog.Report.Elements.Tooltip" ) );
    props.setLook( wQuElements );
    wQuElementsReference = new Link( wReportGroup, SWT.SINGLE );
    wQuElementsReference.setText( BaseMessages.getString( PKG, "OmnitureInputDialog.Report.Reference.Label" ) );
    props.setLook( wQuElementsReference );
    wQuElementsReference.addListener( SWT.Selection, new Listener() {
      @Override
      public void handleEvent( Event ev ) {
        BareBonesBrowserLaunch.openURL( REFERENCE_ELEMENTS_URI );
      }
    } );
    wQuElementsReference.pack( true );
    FormData fdQuElements = new FormData();
    fdQuElements.top = new FormAttachment( wQuDateGranularity, margin );
    fdQuElements.left = new FormAttachment( middle, 0 );
    fdQuElements.right = new FormAttachment( 100, -wQuElementsReference.getBounds().width - margin );
    wQuElements.setLayoutData( fdQuElements );
    FormData fdQuElementsReference = new FormData();
    fdQuElementsReference.top = new FormAttachment( wQuDateGranularity, margin );
    fdQuElementsReference.left = new FormAttachment( wQuElements, 0 );
    fdQuElementsReference.right = new FormAttachment( 100, 0 );
    wQuElementsReference.setLayoutData( fdQuElementsReference );

    // Report metrics
    wlQuMetrics = new Label( wReportGroup, SWT.RIGHT );
    wlQuMetrics.setText( BaseMessages.getString( PKG, "OmnitureInputDialog.Report.Metrics.Label" ) );
    props.setLook( wlQuMetrics );
    FormData fdlQuMetrics = new FormData();
    fdlQuMetrics.top = new FormAttachment( wQuElements, margin );
    fdlQuMetrics.left = new FormAttachment( 0, 0 );
    fdlQuMetrics.right = new FormAttachment( middle, -margin );
    wlQuMetrics.setLayoutData( fdlQuMetrics );
    wQuMetrics = new TextVar( transMeta, wReportGroup, SWT.SINGLE | SWT.LEFT | SWT.BORDER );
    wQuMetrics.addModifyListener( lsMod );
    wQuMetrics.setToolTipText( BaseMessages.getString( PKG, "OmnitureInputDialog.Report.Metrics.Tooltip" ) );
    props.setLook( wQuMetrics );
    wQuMetricsReference = new Link( wReportGroup, SWT.SINGLE );
    wQuMetricsReference.setText( BaseMessages.getString( PKG, "OmnitureInputDialog.Report.Reference.Label" ) );
    props.setLook( wQuMetricsReference );
    wQuMetricsReference.addListener( SWT.Selection, new Listener() {
      @Override
      public void handleEvent( Event ev ) {
        BareBonesBrowserLaunch.openURL( REFERENCE_METRICS_URI );
      }
    } );
    wQuMetricsReference.pack( true );
    FormData fdQuMetrics = new FormData();
    fdQuMetrics.top = new FormAttachment( wQuElements, margin );
    fdQuMetrics.left = new FormAttachment( middle, 0 );
    fdQuMetrics.right = new FormAttachment( 100, -wQuMetricsReference.getBounds().width - margin );
    wQuMetrics.setLayoutData( fdQuMetrics );

    FormData fdQuMetricsReference = new FormData();
    fdQuMetricsReference.top = new FormAttachment( wQuElements, margin );
    fdQuMetricsReference.left = new FormAttachment( wQuMetrics, 0 );
    fdQuMetricsReference.right = new FormAttachment( 100, 0 );
    wQuMetricsReference.setLayoutData( fdQuMetricsReference );

    // Report segments
    wlQuSegments = new Label( wReportGroup, SWT.RIGHT );
    wlQuSegments.setText( BaseMessages.getString( PKG, "OmnitureInputDialog.Report.Segments.Label" ) );
    props.setLook( wlQuSegments );
    FormData fdlQuSegments = new FormData();
    fdlQuSegments.top = new FormAttachment( wQuMetrics, margin );
    fdlQuSegments.left = new FormAttachment( 0, 0 );
    fdlQuSegments.right = new FormAttachment( middle, -margin );
    wlQuSegments.setLayoutData( fdlQuSegments );
    wQuSegments = new TextVar( transMeta, wReportGroup, SWT.SINGLE | SWT.LEFT | SWT.BORDER );
    wQuSegments.addModifyListener( lsMod );
    wQuSegments.setToolTipText( BaseMessages.getString( PKG, "OmnitureInputDialog.Report.Segments.Tooltip" ) );
    props.setLook( wQuSegments );
    wQuSegmentsReference = new Link( wReportGroup, SWT.SINGLE );
    wQuSegmentsReference.setText( BaseMessages.getString( PKG, "OmnitureInputDialog.Report.Reference.Label" ) );
    props.setLook( wQuSegmentsReference );
    wQuSegmentsReference.addListener( SWT.Selection, new Listener() {
      @Override
      public void handleEvent( Event ev ) {
        BareBonesBrowserLaunch.openURL( REFERENCE_SEGMENTS_URI );
      }
    } );
    wQuSegmentsReference.pack( true );
    FormData fdQuSegments = new FormData();
    fdQuSegments.top = new FormAttachment( wQuMetrics, margin );
    fdQuSegments.left = new FormAttachment( middle, 0 );
    fdQuSegments.right = new FormAttachment( 100, -wQuSegmentsReference.getBounds().width - margin );
    wQuSegments.setLayoutData( fdQuSegments );
    FormData fdQuSegmentsReference = new FormData();
    fdQuSegmentsReference.top = new FormAttachment( wQuMetrics, margin );
    fdQuSegmentsReference.left = new FormAttachment( wQuSegments, 0 );
    fdQuSegmentsReference.right = new FormAttachment( 100, 0 );
    wQuSegmentsReference.setLayoutData( fdQuSegmentsReference );
    
    fdReportGroup = new FormData();
    fdReportGroup.left = new FormAttachment( 0, 0 );
    fdReportGroup.right = new FormAttachment( 100, 0 );
    fdReportGroup.top = new FormAttachment( wConnectGroup, 2 * margin );
    wReportGroup.setLayoutData( fdReportGroup );
    
    
    // ////////////////////////
    // START OF FIELDS TAB ///
    // ////////////////////////

    wFieldsTab = new CTabItem( wTabFolder, SWT.NONE );
    wFieldsTab.setText( BaseMessages.getString( PKG, "OmnitureInputDialog.Tab.Fields.Label" ) );

    FormLayout fieldsLayout = new FormLayout();
    fieldsLayout.marginWidth = Const.FORM_MARGIN;
    fieldsLayout.marginHeight = Const.FORM_MARGIN;

    wFieldsComp = new Composite( wTabFolder, SWT.NONE );
    wFieldsComp.setLayout( fieldsLayout );
    props.setLook( wFieldsComp );

    wGet = new Button( wFieldsComp, SWT.PUSH );
    wGet.setText( BaseMessages.getString( PKG, "OmnitureInputDialog.GetFields.Button" ) );
    fdGet = new FormData();
    fdGet.left = new FormAttachment( 50, 0 );
    fdGet.bottom = new FormAttachment( 100, 0 );
    wGet.setLayoutData( fdGet );
    
    wGet.addListener( SWT.Selection, new Listener() {
        @Override
        public void handleEvent( Event e ) {
	        Cursor busy = new Cursor( shell.getDisplay(), SWT.CURSOR_WAIT );
	        shell.setCursor( busy );
	        getFields();
	        shell.setCursor( null );
	        busy.dispose();
        }
      }
    );

    final int FieldsRows = input.getInputFields().length;

    colinf =
      new ColumnInfo[] {
        new ColumnInfo(
          BaseMessages.getString( PKG, "OmnitureInputDialog.FieldsTable.Name.Column" ),
          ColumnInfo.COLUMN_TYPE_TEXT, false ),
        new ColumnInfo(
          BaseMessages.getString( PKG, "OmnitureInputDialog.FieldsTable.Type.Column" ),
          ColumnInfo.COLUMN_TYPE_CCOMBO, ValueMetaBase.getTypes(), true ),
        new ColumnInfo(
          BaseMessages.getString( PKG, "OmnitureInputDialog.FieldsTable.Format.Column" ),
          ColumnInfo.COLUMN_TYPE_FORMAT, 3 ),
        new ColumnInfo(
          BaseMessages.getString( PKG, "OmnitureInputDialog.FieldsTable.Length.Column" ),
          ColumnInfo.COLUMN_TYPE_TEXT, false ),
        new ColumnInfo(
          BaseMessages.getString( PKG, "OmnitureInputDialog.FieldsTable.Precision.Column" ),
          ColumnInfo.COLUMN_TYPE_TEXT, false ),
        new ColumnInfo(
          BaseMessages.getString( PKG, "OmnitureInputDialog.FieldsTable.Currency.Column" ),
          ColumnInfo.COLUMN_TYPE_TEXT, false ),
        new ColumnInfo(
          BaseMessages.getString( PKG, "OmnitureInputDialog.FieldsTable.Decimal.Column" ),
          ColumnInfo.COLUMN_TYPE_TEXT, false ),
        new ColumnInfo(
          BaseMessages.getString( PKG, "OmnitureInputDialog.FieldsTable.Group.Column" ),
          ColumnInfo.COLUMN_TYPE_TEXT, false ),
        new ColumnInfo(
          BaseMessages.getString( PKG, "OmnitureInputDialog.FieldsTable.TrimType.Column" ),
          ColumnInfo.COLUMN_TYPE_CCOMBO, OmnitureInputField.trimTypeDesc, true )
      };

    colinf[0].setUsingVariables( true );
    colinf[0].setToolTip( BaseMessages.getString( PKG, "OmnitureInputDialog.FieldsTable.Name.Column.Tooltip" ) );
    wFields =
      new TableView( transMeta, wFieldsComp, SWT.FULL_SELECTION | SWT.MULTI, colinf, FieldsRows, lsMod, props );

    fdFields = new FormData();
    fdFields.left = new FormAttachment( 0, 0 );
    fdFields.top = new FormAttachment( 0, 0 );
    fdFields.right = new FormAttachment( 100, 0 );
    fdFields.bottom = new FormAttachment( wGet, -margin );
    wFields.setLayoutData( fdFields );

    fdFieldsComp = new FormData();
    fdFieldsComp.left = new FormAttachment( 0, 0 );
    fdFieldsComp.top = new FormAttachment( 0, 0 );
    fdFieldsComp.right = new FormAttachment( 100, 0 );
    fdFieldsComp.bottom = new FormAttachment( 100, 0 );
    wFieldsComp.setLayoutData( fdFieldsComp );

    wFieldsComp.layout();
    wFieldsTab.setControl( wFieldsComp );

    fdTabFolder = new FormData();
    fdTabFolder.left = new FormAttachment( 0, 0 );
    fdTabFolder.top = new FormAttachment( wStepname, margin );
    fdTabFolder.right = new FormAttachment( 100, 0 );
    fdTabFolder.bottom = new FormAttachment( 100, -50 );
    wTabFolder.setLayoutData( fdTabFolder );
    
    /*************************************************
     * // OK AND CANCEL BUTTONS
     *************************************************/

    wOK = new Button( shell, SWT.PUSH );
    wOK.setText( BaseMessages.getString( PKG, "System.Button.OK" ) );
    wCancel = new Button( shell, SWT.PUSH );
    wCancel.setText( BaseMessages.getString( PKG, "System.Button.Cancel" ) );
    wPreview = new Button( shell, SWT.PUSH );
    wPreview.setText( BaseMessages.getString( PKG, "System.Button.Preview" ) );
    wPreview.addListener( SWT.Selection, new Listener() {
        @Override
        public void handleEvent( Event ev ) {
	        Cursor busy = new Cursor( shell.getDisplay(), SWT.CURSOR_WAIT );
	        shell.setCursor( busy );
	        getPreview();
	        shell.setCursor( null );
	        busy.dispose();
        }
      }
    );
    
    setButtonPositions( new Button[] { wOK, wPreview, wCancel }, margin, wTabFolder );
    
    // Add listeners
    lsCancel = new Listener() {
      public void handleEvent( Event e ) {
        cancel();
      }
    };
    lsOK = new Listener() {
      public void handleEvent( Event e ) {
        try {
			ok();
		} catch (KettleException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
      }
    };
    wCancel.addListener( SWT.Selection, lsCancel );
    wOK.addListener( SWT.Selection, lsOK );

    /*************************************************
     * // DEFAULT ACTION LISTENERS
     *************************************************/
    lsDef = new SelectionAdapter() {
      public void widgetDefaultSelected( SelectionEvent e ) {
        try {
			ok();
		} catch (KettleException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
      }
    };
    wStepname.addSelectionListener( lsDef );
    wQuStartDate.addSelectionListener( lsDef );
    wQuEndDate.addSelectionListener( lsDef );
    wQuElements.addSelectionListener( lsDef );
    wQuMetrics.addSelectionListener( lsDef );

    // Detect X or ALT-F4 or something that kills this window...
    shell.addShellListener(
      new ShellAdapter() {
        public void shellClosed( ShellEvent e ) {
          cancel();
        }
      }
    );
    
    /*************************************************
     * // POPULATE AND OPEN DIALOG
     *************************************************/

    // Set the shell size, based upon previous time...
    wTabFolder.setSelection( 0 );
    setSize();
    getData( input );
    input.setChanged( changed );
    wStepname.setFocus();
    shell.open();

    while ( !shell.isDisposed() ) {
      if ( !display.readAndDispatch() ) {
        display.sleep();
      }
    }

    return stepname;
  }

private void getReportSuiteIdsList() {
    if ( !gotReportSuiteIds ) {
      String selectedField = wReportSuiteId.getText();
      wReportSuiteId.removeAll();
      try {
	      OmnitureInputMeta meta = new OmnitureInputMeta();
	      getInfo( meta );
	      // get real values
	      String realUsername = transMeta.environmentSubstitute( meta.getUserName() );
	      String realSecret = transMeta.environmentSubstitute( meta.getSecret() );
		  AnalyticsClient client = new AnalyticsClientBuilder()
				  .setEndpoint("api2.omniture.com")
				  .authenticateWithSecret(realUsername, realSecret)
				  .build();
		  ReportSuiteMethods reportSuiteMethods = new ReportSuiteMethods(client);
		  CompanyReportSuites reportSuiteIds = new CompanyReportSuites();
		  reportSuiteIds = reportSuiteMethods.getReportSuites();
		  String[] reportsuiteids = new String[reportSuiteIds.getReportSuites().size()];
		  for (int i = 0; i < reportSuiteIds.getReportSuites().size(); i++) {
			  reportsuiteids[i] = reportSuiteIds.getReportSuites().get(i).getRsid();
		  }
          if ( reportsuiteids != null && reportsuiteids.length > 0 ) {
            // populate combo
            wReportSuiteId.setItems( reportsuiteids );
          }
          gotReportSuiteIds = true;
          getReportSuiteIdsListError = false;
      } catch ( Exception e ) {
        new ErrorDialog( shell, BaseMessages.getString(
          PKG, "OmnitureInputDialog.ErrorRetrieveReportSuiteIds.DialogTitle" ), BaseMessages.getString(
          PKG, "OmnitureInputDialog.ErrorRetrieveData.ErrorRetrieveReportSuiteIds" ), e );
        getReportSuiteIdsListError = true;
      } finally {
        if ( !Const.isEmpty( selectedField ) ) {
          wReportSuiteId.setText( selectedField );
        }
      }
    }
  }
  
  private void testConnection() {

	    boolean successConnection = true;
	    String msgError = null;
	    CompanyReportSuites reportSuiteIds = new CompanyReportSuites();
	    try {
	      OmnitureInputMeta meta = new OmnitureInputMeta();
	      getInfo( meta );
	      // get real values
	      String realUsername = transMeta.environmentSubstitute( meta.getUserName() );
	      String realSecret = transMeta.environmentSubstitute( meta.getSecret() );
		  AnalyticsClient client = new AnalyticsClientBuilder()
				  .setEndpoint("api2.omniture.com")
				  .authenticateWithSecret(realUsername, realSecret)
				  .build();
		  ReportSuiteMethods reportSuiteMethods = new ReportSuiteMethods(client);
		  reportSuiteIds = reportSuiteMethods.getReportSuites();
		  if(reportSuiteIds.getReportSuites().size() < 1){
			  successConnection = false;  
		  }
	    } catch ( Exception e ) {
	      successConnection = false;
	      msgError = e.getMessage();
	    }
	    if ( successConnection ) {
	      MessageBox mb = new MessageBox( shell, SWT.OK | SWT.ICON_INFORMATION );
	      mb.setMessage( BaseMessages.getString( PKG, "OmnitureInputDialog.Connected.OK", 
	    		  wUserName.getText(), reportSuiteIds.getReportSuites().size())
	        + Const.CR );
	      mb.setText( BaseMessages.getString( PKG, "OmnitureInputDialog.Connected.Title.Ok" ) );
	      mb.open();
	    } else {
	    	if(msgError == null){
	  	      MessageBox mb = new MessageBox( shell, SWT.OK | SWT.ICON_INFORMATION );
		      mb.setMessage( BaseMessages.getString( PKG, "OmnitureInputDialog.Connected.NoReportSuites", 
		    		  wUserName.getText(), reportSuiteIds.getReportSuites().size())
		        + Const.CR );
		      mb.setText( BaseMessages.getString( PKG, "OmnitureInputDialog.Connected.Title.Error" ) );
		      mb.open();
	    	} else {
		      new ErrorDialog(
		  	        shell,
		  	        BaseMessages.getString( PKG, "OmnitureInputDialog.Connected.Title.Error" ),
		  	        BaseMessages.getString( PKG, "OmnitureInputDialog.Connected.NOK", wUserName.getText() ),
		  	        new Exception( msgError ) );
		  	    }
	    }
  }

  private void getFields() {
	  
	    try {
	      OmnitureInputMeta meta = new OmnitureInputMeta();
	      getInfo( meta );
	      // clear the current fields grid
	      wFields.removeAll();
	      // get real values
	      String realUsername = transMeta.environmentSubstitute( meta.getUserName() );
	      String realSecret = transMeta.environmentSubstitute( meta.getSecret() );
	      String realReportSuiteId = transMeta.environmentSubstitute( meta.getReportSuiteId() );
	      String realStartDate = transMeta.environmentSubstitute( meta.getStartDate() );
	      String realEndDate = transMeta.environmentSubstitute( meta.getEndDate() );
	      String realDateGranularity = transMeta.environmentSubstitute( meta.getDateGranularity() );
	      String realElements = transMeta.environmentSubstitute( meta.getElements() );
	      String realMetrics = transMeta.environmentSubstitute( meta.getMetrics() );
	      String realSegments = transMeta.environmentSubstitute( meta.getSegments() );
			
		  AnalyticsClient client = new AnalyticsClientBuilder()
				  .setEndpoint("api2.omniture.com")
				  .authenticateWithSecret(realUsername, realSecret)
				  .build();
		  ReportDescription desc = new ReportDescription();
		  desc.setReportSuiteID(realReportSuiteId);
		  desc.setDateFrom(realStartDate); 
		  desc.setDateTo(realEndDate);
	      // parse lists of elements, metrics and segments
		    List<ReportDescriptionMetric> descMetrics = new ArrayList<>();
			for (String id : realMetrics.split(",")) {
				ReportDescriptionMetric metric = new ReportDescriptionMetric();
				metric.setId(id);
				descMetrics.add(metric);
			}
			desc.setMetrics(descMetrics);
			if(!realElements.equals("")){
				List<ReportDescriptionElement> descElems = new ArrayList<>();
				for (String id : realElements.split(",")) {
					ReportDescriptionElement elem = new ReportDescriptionElement();
					elem.setId(id);
					descElems.add(elem);
				}
				desc.setElements(descElems);
			}
			if(!realSegments.equals("")){
				List<ReportDescriptionSegment> descSegments = new ArrayList<>();
				for (String id : realSegments.split(",")) {
					ReportDescriptionSegment seg = new ReportDescriptionSegment();
					seg.setId(id);
					descSegments.add(seg);
				}
				desc.setSegments(descSegments);
			}
		  if(!realDateGranularity.equals("")) {
			 desc.setDateGranularity(ReportDescriptionDateGranularity.valueOf(realDateGranularity));
		  }
		  ReportMethods reportMethods = new ReportMethods(client);
		  int reportId = 0;
		  ReportResponse response = null;
		  try {
		  reportId = reportMethods.queue(desc);
		  while (response == null) {
		  try {
		      response = reportMethods.get(reportId);
		  } catch (ApiException e) {
		      if ("report_not_ready".equals(e.getError())) {
		          System.err.println("Report not ready yet.");
		          try {
		  			Thread.sleep(3000);
		  		} catch (InterruptedException e1) {
		  			// TODO Auto-generated catch block
		  			e1.printStackTrace();
		  		}
		          continue;
		      }
		      throw e;
		  }
		  }
		  } catch (IOException i) {
		  System.err.println("Report queuing error.");
		  }
		  /* Get the report */
		  Report report = response.getReport();
		  List<String> headerNames = report.getHeaders();
	      getTableView().table.setItemCount( headerNames.size() );
	      for (int j = 0; j < headerNames.size(); j++) 
	      {
	        TableItem item = getTableView().table.getItem( j );
	        item.setText( 1, headerNames.get(j));
	      }
	      wFields.removeEmptyRows();
	      wFields.setRowNums();
	      wFields.optWidth( true );
	      getInput().setChanged();
	    } catch ( Exception e ) {
	      new ErrorDialog(
	        shell, BaseMessages.getString( PKG, "OmnitureInputMeta.ErrorRetrieveData.DialogTitle" ), BaseMessages
	          .getString( PKG, "OmnitureInputMeta.ErrorRetrieveData.DialogMessage" ), e );
	  }
  }
 
  // Preview the data
  private void getPreview() {
    try {
    	
      OmnitureInputMeta oneMeta = new OmnitureInputMeta();
      getInfo( oneMeta );

      // check if the path is given

      TransMeta previewMeta =
        TransPreviewFactory.generatePreviewTransformation( transMeta, oneMeta, wStepname.getText() );

      EnterNumberDialog numberDialog = new EnterNumberDialog( shell, props.getDefaultPreviewSize(),
        BaseMessages.getString( PKG, "OmnitureInputDialog.NumberRows.DialogTitle" ),
        BaseMessages.getString( PKG, "OmnitureInputDialog.NumberRows.DialogMessage" ) );
      int previewSize = numberDialog.open();
      if ( previewSize > 0 ) {
        TransPreviewProgressDialog progressDialog =
          new TransPreviewProgressDialog(
            shell, previewMeta, new String[] { wStepname.getText() }, new int[] { previewSize } );
        progressDialog.open();

        if ( !progressDialog.isCancelled() ) {
          Trans trans = progressDialog.getTrans();
          String loggingText = progressDialog.getLoggingText();

          if ( trans.getResult() != null && trans.getResult().getNrErrors() > 0 ) {
            EnterTextDialog etd =
              new EnterTextDialog(
                shell, BaseMessages.getString( PKG, "System.Dialog.PreviewError.Title" ), BaseMessages
                  .getString( PKG, "System.Dialog.PreviewError.Message" ), loggingText, true );
            etd.setReadOnly();
            etd.open();
          }

          PreviewRowsDialog prd =
            new PreviewRowsDialog(
              shell, transMeta, SWT.NONE, wStepname.getText(), progressDialog.getPreviewRowsMeta( wStepname
                .getText() ), progressDialog.getPreviewRows( wStepname.getText() ), loggingText );
          prd.open();
        }
      }
    } catch ( Exception e ) {
      new ErrorDialog( shell, BaseMessages
        .getString( PKG, "OmnitureInputDialog.ErrorPreviewingData.DialogTitle" ), BaseMessages.getString(
        PKG, "OmnitureInputDialog.ErrorPreviewingData.DialogMessage" ), e );
    }
  }

private void getInfo( OmnitureInputMeta in ) {

    stepname = wStepname.getText(); // return value
    
    in.setUserName( wUserName.getText() );
    in.setSecret( wSecret.getText() );
    in.setReportSuiteId( wReportSuiteId.getText() );
    in.setStartDate( wQuStartDate.getText() );
    in.setEndDate( wQuEndDate.getText() );
    in.setDateGranularity( wQuDateGranularity.getText() );
    in.setElements( wQuElements.getText() );
    in.setMetrics( wQuMetrics.getText() );
    in.setSegments( wQuSegments.getText() );

    int nrFields = getTableView().nrNonEmpty();

    in.allocate( nrFields );

    for ( int i = 0; i < nrFields; i++ ) {
      OmnitureInputField field = new OmnitureInputField();

      TableItem item = wFields.getNonEmpty( i );

      field.setName( item.getText( 1 ) );
      field.setType( ValueMetaBase.getType( item.getText( 2 ) ) );
      field.setFormat( item.getText( 3 ) );
      field.setLength( Const.toInt( item.getText( 4 ), -1 ) );
      field.setPrecision( Const.toInt( item.getText( 5 ), -1 ) );
      field.setCurrencySymbol( item.getText( 6 ) );
      field.setDecimalSymbol( item.getText( 7 ) );
      field.setGroupSymbol( item.getText( 8 ) );
      field.setTrimType( OmnitureInputField.getTrimTypeByDesc( item.getText( 9 ) ) );
      
      in.getInputFields()[i] = field;
    }
  }

  /**
   * Read the data from the OmnitureInputMeta object and show it in this dialog.
   * @param in The OmnitureInputMeta object to obtain the data from.
   */
  public void getData( OmnitureInputMeta in ) {
	  
    wUserName.setText( Const.NVL( in.getUserName(), "" ) );
    wSecret.setText( Const.NVL( in.getSecret(), "" ) );
    wReportSuiteId.setText( Const.NVL( in.getReportSuiteId(), "" ) );
    wQuStartDate.setText( Const.NVL( in.getStartDate(), "" ) );
    wQuEndDate.setText( Const.NVL( in.getEndDate(), "" ) );
    wQuDateGranularity.setText( Const.NVL( in.getDateGranularity(), "" ) );
    wQuElements.setText( Const.NVL( in.getElements(), "" ) );
    wQuMetrics.setText( Const.NVL( in.getMetrics(), "" ) );
    wQuSegments.setText( Const.NVL( in.getSegments(), "" ) );
    
    if ( log.isDebug() ) {
      logDebug( BaseMessages.getString( PKG, "OmnitureInputDialog.Log.GettingFieldsInfo" ) );
    }
    
    for ( int i = 0; i < in.getInputFields().length; i++ ) {
    	OmnitureInputField field = in.getInputFields()[i];

      if ( field != null ) {
        TableItem item = wFields.table.getItem( i );
        String name = field.getName();
        String type = field.getTypeDesc();
        String format = field.getFormat();
        String length = "" + field.getLength();
        String prec = "" + field.getPrecision();
        String curr = field.getCurrencySymbol();
        String decim = field.getDecimalSymbol();
        String group = field.getGroupSymbol();
        String trim = field.getTrimTypeDesc();
        
        if ( name != null ) {
          item.setText( 1, name );
        }
        if ( type != null ) {
          item.setText( 2, type );
        }
        if ( format != null ) {
          item.setText( 3, format );
        }
        if ( length != null && !"-1".equals( length ) ) {
          item.setText( 4, length );
        }
        if ( prec != null && !"-1".equals( prec ) ) {
          item.setText( 5, prec );
        }
        if ( curr != null ) {
          item.setText( 6, curr );
        }
        if ( decim != null ) {
          item.setText( 7, decim );
        }
        if ( group != null ) {
          item.setText( 8, group );
        }
        if ( trim != null ) {
          item.setText( 9, trim );
        }
      }
    }

    wFields.removeEmptyRows();
    wFields.setRowNums();
    wFields.optWidth( true );

    wStepname.selectAll();
    wStepname.setFocus();
  }

  private void cancel() {
    stepname = null;
    input.setChanged( changed );
    dispose();
  }

  private void ok() throws KettleException {
    if ( Const.isEmpty( wStepname.getText() ) ) {
      return;
    }
    stepname = wStepname.getText();
    getInfo( input );
    dispose();
  }

  TableView getTableView() {
    return wFields;
  }

  void setTableView( TableView wFields ) {
    this.wFields = wFields;
  }

  OmnitureInputMeta getInput() {
    return input;
  }

  void setInput( OmnitureInputMeta input ) {
    this.input = input;
  }
}
