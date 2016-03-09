/* ******************************************************************************
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

package org.pentaho.di.ui.trans.steps.solrinput;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.response.SolrPingResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.params.CursorMarkParams;
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
import org.pentaho.di.core.Const;
import org.pentaho.di.core.Props;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.row.value.ValueMetaBase;
import org.pentaho.di.i18n.BaseMessages;
import org.pentaho.di.trans.TransPreviewFactory;
import org.pentaho.di.trans.step.BaseStepMeta;
import org.pentaho.di.trans.step.StepDialogInterface;
import org.pentaho.di.trans.steps.solrinput.SolrInputField;
import org.pentaho.di.trans.steps.solrinput.SolrInputMeta;
import org.pentaho.di.ui.trans.steps.solrinput.BareBonesBrowserLaunch;
import org.pentaho.di.ui.core.widget.LabelTextVar;
import org.pentaho.di.ui.core.widget.TableView;
import org.pentaho.di.ui.core.widget.TextVar;
import org.pentaho.di.ui.trans.step.BaseStepDialog;

public class SolrInputDialog extends BaseStepDialog implements StepDialogInterface {

  private static Class<?> PKG = SolrInputMeta.class; // for i18n purposes

  private SolrInputMeta input;

  private CTabFolder wTabFolder;
  private Composite wSetupComp, wFieldsComp;
  private CTabItem wSetupTab, wFieldsTab;
  
  private FormData fdTabFolder, fdFieldsComp;
  private FormData fdFields;
  
  private Group wConnectGroup;
  private FormData fdConnectGroup;
  private Group wQueryGroup;
  private FormData fdQueryGroup;
  
  private LabelTextVar wURL;
  private FormData fdURL;
  private Label wlQ, wlSort, wlFq, wlFl, wlFacetField, wlFacetQuery;
  private TextVar wQ, wSort, wFq, wFl, wFacetField, wFacetQuery;
  private Label wlCursor;
  private CCombo wQuCursor;
  
  private Link wQReference, wSortReference, wFqReference, wFlReference, wFacetFieldReference, wFacetQueryReference;
  
  private Button wTest;
  private FormData fdTest;

  private TableView wFields;
  
  private int middle;
  private int margin;

  private ColumnInfo[] colinf;

  private ModifyListener lsMod;
  
  public static final String[] cursorYN = { "No", "Yes" };

  static final String REFERENCE_Q_URI =
    "https://wiki.apache.org/solr/CommonQueryParameters#q";
  static final String REFERENCE_SORT_URI =
	"https://wiki.apache.org/solr/CommonQueryParameters#sort";
  static final String REFERENCE_FQ_URI =
    "https://wiki.apache.org/solr/CommonQueryParameters#fq";
  static final String REFERENCE_FL_URI =
    "https://wiki.apache.org/solr/CommonQueryParameters#fl";
  static final String REFERENCE_FACETFIELD_URI =
    "https://wiki.apache.org/solr/SimpleFacetParameters#facet.field";
  static final String REFERENCE_FACETQUERY_URI =
    "https://wiki.apache.org/solr/SimpleFacetParameters#facet.query_:_Arbitrary_Query_Faceting";
  
  // constructor
  public SolrInputDialog( Shell parent, Object in, TransMeta transMeta, String sname ) {
    super( parent, (BaseStepMeta) in, transMeta, sname );
    setInput( (SolrInputMeta) in );
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
    shell.setText( BaseMessages.getString( PKG, "SolrInputDialog.Shell.Title" ) );

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
    wSetupTab.setText( BaseMessages.getString( PKG, "SolrInputDialog.Tab.Setup.Label" ) );
    wSetupComp = new Composite( wTabFolder, SWT.NONE );
    props.setLook( wSetupComp );
    FormLayout generalLayout = new FormLayout();
    generalLayout.marginWidth = 3;
    generalLayout.marginHeight = 3;
    wSetupComp.setLayout( generalLayout );
    wSetupTab.setControl( wSetupComp );
    
    /*************************************************
     * // SOLR CONNECTION GROUP
     *************************************************/
    wConnectGroup = new Group( wSetupComp, SWT.SHADOW_ETCHED_IN );
    wConnectGroup.setText( BaseMessages.getString( PKG, "SolrInputDialog.ConnectGroup.Label" ) );
    FormLayout fconnLayout = new FormLayout();
    fconnLayout.marginWidth = 3;
    fconnLayout.marginHeight = 3;
    wConnectGroup.setLayout( fconnLayout );
    props.setLook( wConnectGroup );

    // URL line
    wURL = new LabelTextVar( transMeta, wConnectGroup,
      BaseMessages.getString( PKG, "SolrInputDialog.URL.Label" ),
      BaseMessages.getString( PKG, "SolrInputDialog.URL.Tooltip" ) );
    props.setLook( wURL );
    wURL.addModifyListener( lsMod );
    fdURL = new FormData();
    fdURL.left = new FormAttachment( 0, 0 );
    fdURL.top = new FormAttachment( 0, margin );
    fdURL.right = new FormAttachment( 100, 0 );
    wURL.setLayoutData( fdURL );
    
    // Test Solr connection button
    wTest = new Button( wConnectGroup, SWT.PUSH );
    wTest.setText( BaseMessages.getString( PKG, "SolrInputDialog.TestConnection.Label" ) );
    props.setLook( wTest );
    fdTest = new FormData();
    wTest.setToolTipText( BaseMessages.getString( PKG, "SolrInputDialog.TestConnection.Tooltip" ) );
    // fdTest.left = new FormAttachment(middle, 0);
    fdTest.top = new FormAttachment( wURL, margin );
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
     * // SOLR QUERY DEFINITION
     *************************************************/
    
    wQueryGroup = new Group( wSetupComp, SWT.SHADOW_ETCHED_IN );
    wQueryGroup.setText( BaseMessages.getString( PKG, "SolrInputDialog.QueryGroup.Label" ) );
    FormLayout freportLayout = new FormLayout();
    freportLayout.marginWidth = 3;
    freportLayout.marginHeight = 3;
    wQueryGroup.setLayout( freportLayout );
    props.setLook( wQueryGroup );
    
    // q line
    wlQ = new Label( wQueryGroup, SWT.RIGHT );
    wlQ.setText( BaseMessages.getString( PKG, "SolrInputDialog.Query.Q.Label" ) );
    props.setLook( wlQ );
    FormData fdlQuQ = new FormData();
    fdlQuQ.top = new FormAttachment( wQueryGroup, 2 * margin );
    fdlQuQ.left = new FormAttachment( 0, 0 );
    fdlQuQ.right = new FormAttachment( middle, -margin );
    wlQ.setLayoutData( fdlQuQ );
    
    wQ = new TextVar( transMeta, wQueryGroup, SWT.SINGLE | SWT.LEFT | SWT.BORDER );
    wQ.addModifyListener( lsMod );
    wQ.setToolTipText( BaseMessages.getString( PKG, "SolrInputDialog.Query.Q.Tooltip" ) );
    props.setLook( wQ );
    wQReference = new Link( wQueryGroup, SWT.SINGLE );
    wQReference.setText( BaseMessages.getString( PKG, "SolrInputDialog.Query.Reference.Label" ) );
    props.setLook( wQReference );
    wQReference.addListener( SWT.Selection, new Listener() {
      @Override
      public void handleEvent( Event ev ) {
        BareBonesBrowserLaunch.openURL( REFERENCE_Q_URI );
      }
    } );
    wQReference.pack( true );
    FormData fdQuQ = new FormData();
    fdQuQ.top = new FormAttachment( wQueryGroup, 2 * margin );
    fdQuQ.left = new FormAttachment( middle, 0 );
    fdQuQ.right = new FormAttachment( 100, -wQReference.getBounds().width - margin );
    wQ.setLayoutData( fdQuQ );
    FormData fdQuQReference = new FormData();
    fdQuQReference.top = new FormAttachment( wQueryGroup, 2 * margin );
    fdQuQReference.left = new FormAttachment( wQ, 0 );
    fdQuQReference.right = new FormAttachment( 100, 0 );
    wQReference.setLayoutData( fdQuQReference );
    
    // sort line
    wlSort = new Label( wQueryGroup, SWT.RIGHT );
    wlSort.setText( BaseMessages.getString( PKG, "SolrInputDialog.Query.Sort.Label" ) );
    props.setLook( wlSort );
    FormData fdlQuSort = new FormData();
    fdlQuSort.top = new FormAttachment( wQ, 2 * margin );
    fdlQuSort.left = new FormAttachment( 0, 0 );
    fdlQuSort.right = new FormAttachment( middle, -margin );
    wlSort.setLayoutData( fdlQuSort );
    
    wSort = new TextVar( transMeta, wQueryGroup, SWT.SINGLE | SWT.LEFT | SWT.BORDER );
    wSort.addModifyListener( lsMod );
    wSort.setToolTipText( BaseMessages.getString( PKG, "SolrInputDialog.Query.Sort.Tooltip" ) );
    props.setLook( wSort );
    wSortReference = new Link( wQueryGroup, SWT.SINGLE );
    wSortReference.setText( BaseMessages.getString( PKG, "SolrInputDialog.Query.Reference.Label" ) );
    props.setLook( wSortReference );
    wSortReference.addListener( SWT.Selection, new Listener() {
      @Override
      public void handleEvent( Event ev ) {
        BareBonesBrowserLaunch.openURL( REFERENCE_SORT_URI );
      }
    } );
    wSortReference.pack( true );
    FormData fdQuSort = new FormData();
    fdQuSort.top = new FormAttachment( wQ, 2 * margin );
    fdQuSort.left = new FormAttachment( middle, 0 );
    fdQuSort.right = new FormAttachment( 100, -wSortReference.getBounds().width - margin );
    wSort.setLayoutData( fdQuSort );
    FormData fdQuSortReference = new FormData();
    fdQuSortReference.top = new FormAttachment( wQ, 2 * margin );
    fdQuSortReference.left = new FormAttachment( wSort, 0 );
    fdQuSortReference.right = new FormAttachment( 100, 0 );
    wSortReference.setLayoutData( fdQuSortReference );
    
    // option for cursor paging
    wlCursor = new Label( wQueryGroup, SWT.RIGHT );
    wlCursor.setText( BaseMessages.getString( PKG, "SolrInputDialog.Query.Cursor.Label" ) );
    props.setLook( wlCursor );
    FormData fdlCursor = new FormData();
    fdlCursor.top = new FormAttachment( wSort, margin );
    fdlCursor.left = new FormAttachment( 0, 0 );
    fdlCursor.right = new FormAttachment( middle, -margin );
    wlCursor.setLayoutData( fdlCursor );
    wQuCursor = new CCombo( wQueryGroup, SWT.BORDER | SWT.READ_ONLY );
    props.setLook( wQuCursor );
    wQuCursor.addModifyListener( lsMod );
    FormData fdCursor = new FormData();
    fdCursor.top = new FormAttachment( wSort, margin );
    fdCursor.left = new FormAttachment( middle, 0 );
    fdCursor.right = new FormAttachment( 100, 0 );
    wQuCursor.setLayoutData( fdCursor );
    wQuCursor.setItems( cursorYN );
    
    // fq 
    wlFq = new Label( wQueryGroup, SWT.RIGHT );
    wlFq.setText( BaseMessages.getString( PKG, "SolrInputDialog.Query.Fq.Label" ) );
    props.setLook( wlFq );
    FormData fdlQuFq = new FormData();
    fdlQuFq.top = new FormAttachment( wQuCursor, margin );
    fdlQuFq.left = new FormAttachment( 0, 0 );
    fdlQuFq.right = new FormAttachment( middle, -margin );
    wlFq.setLayoutData( fdlQuFq );
    wFq = new TextVar( transMeta, wQueryGroup, SWT.SINGLE | SWT.LEFT | SWT.BORDER );
    wFq.addModifyListener( lsMod );
    wFq.setToolTipText( BaseMessages.getString( PKG, "SolrInputDialog.Query.Fq.Tooltip" ) );
    props.setLook( wFq );
    wFqReference = new Link( wQueryGroup, SWT.SINGLE );
    wFqReference.setText( BaseMessages.getString( PKG, "SolrInputDialog.Query.Reference.Label" ) );
    props.setLook( wFqReference );
    wFqReference.addListener( SWT.Selection, new Listener() {
      @Override
      public void handleEvent( Event ev ) {
        BareBonesBrowserLaunch.openURL( REFERENCE_FQ_URI );
      }
    } );
    wFqReference.pack( true );
    FormData fdQuFq = new FormData();
    fdQuFq.top = new FormAttachment( wQuCursor, margin );
    fdQuFq.left = new FormAttachment( middle, 0 );
    fdQuFq.right = new FormAttachment( 100, -wFqReference.getBounds().width - margin );
    wFq.setLayoutData( fdQuFq );
    FormData fdQuFqReference = new FormData();
    fdQuFqReference.top = new FormAttachment( wQuCursor, margin );
    fdQuFqReference.left = new FormAttachment( wFq, 0 );
    fdQuFqReference.right = new FormAttachment( 100, 0 );
    wFqReference.setLayoutData( fdQuFqReference );
    
    // fl
    wlFl = new Label( wQueryGroup, SWT.RIGHT );
    wlFl.setText( BaseMessages.getString( PKG, "SolrInputDialog.Query.Fl.Label" ) );
    props.setLook( wlFl );
    FormData fdlQuFl = new FormData();
    fdlQuFl.top = new FormAttachment( wFq, margin );
    fdlQuFl.left = new FormAttachment( 0, 0 );
    fdlQuFl.right = new FormAttachment( middle, -margin );
    wlFl.setLayoutData( fdlQuFl );
    wFl = new TextVar( transMeta, wQueryGroup, SWT.SINGLE | SWT.LEFT | SWT.BORDER );
    wFl.addModifyListener( lsMod );
    wFl.setToolTipText( BaseMessages.getString( PKG, "SolrInputDialog.Query.Fl.Tooltip" ) );
    props.setLook( wFl );
    wFlReference = new Link( wQueryGroup, SWT.SINGLE );
    wFlReference.setText( BaseMessages.getString( PKG, "SolrInputDialog.Query.Reference.Label" ) );
    props.setLook( wFlReference );
    wFlReference.addListener( SWT.Selection, new Listener() {
      @Override
      public void handleEvent( Event ev ) {
        BareBonesBrowserLaunch.openURL( REFERENCE_FL_URI );
      }
    } );
    wFlReference.pack( true );
    FormData fdQuFl = new FormData();
    fdQuFl.top = new FormAttachment( wFq, margin );
    fdQuFl.left = new FormAttachment( middle, 0 );
    fdQuFl.right = new FormAttachment( 100, -wFlReference.getBounds().width - margin );
    wFl.setLayoutData( fdQuFl );
    FormData fdQuFlReference = new FormData();
    fdQuFlReference.top = new FormAttachment( wFq, margin );
    fdQuFlReference.left = new FormAttachment( wFl, 0 );
    fdQuFlReference.right = new FormAttachment( 100, 0 );
    wFlReference.setLayoutData( fdQuFlReference );
    
    // facetField
    wlFacetField = new Label( wQueryGroup, SWT.RIGHT );
    wlFacetField.setText( BaseMessages.getString( PKG, "SolrInputDialog.Query.FacetField.Label" ) );
    props.setLook( wlFacetField );
    FormData fdlQuFacetField = new FormData();
    fdlQuFacetField.top = new FormAttachment( wFl, margin );
    fdlQuFacetField.left = new FormAttachment( 0, 0 );
    fdlQuFacetField.right = new FormAttachment( middle, -margin );
    wlFacetField.setLayoutData( fdlQuFacetField );
    wFacetField = new TextVar( transMeta, wQueryGroup, SWT.SINGLE | SWT.LEFT | SWT.BORDER );
    wFacetField.addModifyListener( lsMod );
    wFacetField.setToolTipText( BaseMessages.getString( PKG, "SolrInputDialog.Query.FacetField.Tooltip" ) );
    props.setLook( wFacetField );
    wFacetFieldReference = new Link( wQueryGroup, SWT.SINGLE );
    wFacetFieldReference.setText( BaseMessages.getString( PKG, "SolrInputDialog.Query.Reference.Label" ) );
    props.setLook( wFacetFieldReference );
    wFacetFieldReference.addListener( SWT.Selection, new Listener() {
      @Override
      public void handleEvent( Event ev ) {
        BareBonesBrowserLaunch.openURL( REFERENCE_FACETFIELD_URI );
      }
    } );
    wFacetFieldReference.pack( true );
    FormData fdQuFacetField = new FormData();
    fdQuFacetField.top = new FormAttachment( wFl, margin );
    fdQuFacetField.left = new FormAttachment( middle, 0 );
    fdQuFacetField.right = new FormAttachment( 100, -wFacetFieldReference.getBounds().width - margin );
    wFacetField.setLayoutData( fdQuFacetField );
    FormData fdQuFacetFieldReference = new FormData();
    fdQuFacetFieldReference.top = new FormAttachment( wFl, margin );
    fdQuFacetFieldReference.left = new FormAttachment( wFacetField, 0 );
    fdQuFacetFieldReference.right = new FormAttachment( 100, 0 );
    wFacetFieldReference.setLayoutData( fdQuFacetFieldReference );
    
    // facetQuery
    wlFacetQuery = new Label( wQueryGroup, SWT.RIGHT );
    wlFacetQuery.setText( BaseMessages.getString( PKG, "SolrInputDialog.Query.FacetQuery.Label" ) );
    props.setLook( wlFacetQuery );
    FormData fdlQuFacetQuery = new FormData();
    fdlQuFacetQuery.top = new FormAttachment( wFacetField, margin );
    fdlQuFacetQuery.left = new FormAttachment( 0, 0 );
    fdlQuFacetQuery.right = new FormAttachment( middle, -margin );
    wlFacetQuery.setLayoutData( fdlQuFacetQuery );
    wFacetQuery = new TextVar( transMeta, wQueryGroup, SWT.SINGLE | SWT.LEFT | SWT.BORDER );
    wFacetQuery.addModifyListener( lsMod );
    wFacetQuery.setToolTipText( BaseMessages.getString( PKG, "SolrInputDialog.Query.FacetQuery.Tooltip" ) );
    props.setLook( wFacetQuery );
    wFacetQueryReference = new Link( wQueryGroup, SWT.SINGLE );
    wFacetQueryReference.setText( BaseMessages.getString( PKG, "SolrInputDialog.Query.Reference.Label" ) );
    props.setLook( wFacetQueryReference );
    wFacetQueryReference.addListener( SWT.Selection, new Listener() {
      @Override
      public void handleEvent( Event ev ) {
        BareBonesBrowserLaunch.openURL( REFERENCE_FACETQUERY_URI );
      }
    } );
    wFacetQueryReference.pack( true );
    FormData fdQuFacetQuery = new FormData();
    fdQuFacetQuery.top = new FormAttachment( wFacetField, margin );
    fdQuFacetQuery.left = new FormAttachment( middle, 0 );
    fdQuFacetQuery.right = new FormAttachment( 100, -wFacetQueryReference.getBounds().width - margin );
    wFacetQuery.setLayoutData( fdQuFacetQuery );
    FormData fdQuFacetQueryReference = new FormData();
    fdQuFacetQueryReference.top = new FormAttachment( wFacetField, margin );
    fdQuFacetQueryReference.left = new FormAttachment( wFacetQuery, 0 );
    fdQuFacetQueryReference.right = new FormAttachment( 100, 0 );
    wFacetQueryReference.setLayoutData( fdQuFacetQueryReference );
    
    
    fdQueryGroup = new FormData();
    fdQueryGroup.left = new FormAttachment( 0, 0 );
    fdQueryGroup.right = new FormAttachment( 100, 0 );
    fdQueryGroup.top = new FormAttachment( wConnectGroup, 2 * margin );
    wQueryGroup.setLayoutData( fdQueryGroup );
    
    
    // ////////////////////////
    // START OF FIELDS TAB ///
    // ////////////////////////

    wFieldsTab = new CTabItem( wTabFolder, SWT.NONE );
    wFieldsTab.setText( BaseMessages.getString( PKG, "SolrInputDialog.Tab.Fields.Label" ) );

    FormLayout fieldsLayout = new FormLayout();
    fieldsLayout.marginWidth = Const.FORM_MARGIN;
    fieldsLayout.marginHeight = Const.FORM_MARGIN;

    wFieldsComp = new Composite( wTabFolder, SWT.NONE );
    wFieldsComp.setLayout( fieldsLayout );
    props.setLook( wFieldsComp );

    wGet = new Button( wFieldsComp, SWT.PUSH );
    wGet.setText( BaseMessages.getString( PKG, "SolrInputDialog.GetFields.Button" ) );
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
          BaseMessages.getString( PKG, "SolrInputDialog.FieldsTable.Name.Column" ),
          ColumnInfo.COLUMN_TYPE_TEXT, false ),
        new ColumnInfo(
          BaseMessages.getString( PKG, "SolrInputDialog.FieldsTable.Type.Column" ),
          ColumnInfo.COLUMN_TYPE_CCOMBO, ValueMetaBase.getTypes(), true ),
        new ColumnInfo(
          BaseMessages.getString( PKG, "SolrInputDialog.FieldsTable.Format.Column" ),
          ColumnInfo.COLUMN_TYPE_FORMAT, 3 ),
        new ColumnInfo(
          BaseMessages.getString( PKG, "SolrInputDialog.FieldsTable.Length.Column" ),
          ColumnInfo.COLUMN_TYPE_TEXT, false ),
        new ColumnInfo(
          BaseMessages.getString( PKG, "SolrInputDialog.FieldsTable.Precision.Column" ),
          ColumnInfo.COLUMN_TYPE_TEXT, false ),
        new ColumnInfo(
          BaseMessages.getString( PKG, "SolrInputDialog.FieldsTable.Currency.Column" ),
          ColumnInfo.COLUMN_TYPE_TEXT, false ),
        new ColumnInfo(
          BaseMessages.getString( PKG, "SolrInputDialog.FieldsTable.Decimal.Column" ),
          ColumnInfo.COLUMN_TYPE_TEXT, false ),
        new ColumnInfo(
          BaseMessages.getString( PKG, "SolrInputDialog.FieldsTable.Group.Column" ),
          ColumnInfo.COLUMN_TYPE_TEXT, false ),
        new ColumnInfo(
          BaseMessages.getString( PKG, "SolrInputDialog.FieldsTable.TrimType.Column" ),
          ColumnInfo.COLUMN_TYPE_CCOMBO, SolrInputField.trimTypeDesc, true )
      };

    colinf[0].setUsingVariables( true );
    colinf[0].setToolTip( BaseMessages.getString( PKG, "SolrInputDialog.FieldsTable.Name.Column.Tooltip" ) );
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
    wQ.addSelectionListener( lsDef );
    wSort.addSelectionListener( lsDef );
    wFq.addSelectionListener( lsDef );
    wFl.addSelectionListener( lsDef );
    wFacetField.addSelectionListener( lsDef );
    wFacetQuery.addSelectionListener( lsDef );

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

//http://www.programcreek.com/java-api-examples/index.php?api=org.apache.solr.client.solrj.response.SolrPingResponse
  private void testConnection() {
	    boolean successConnection = true;
	    String msgError = null;
		try {
	        SolrInputMeta meta = new SolrInputMeta();
	        getInfo( meta );
	        String realURL = transMeta.environmentSubstitute( meta.getURL() );
	        HttpSolrServer solr = new HttpSolrServer(realURL);
			SolrPingResponse response = solr.ping();
		    if (!((String) response.getResponse().get("status")).equals("OK")) {
		    	successConnection = false;
		    }
		} catch (Exception e) {
		      successConnection = false;
		      msgError = e.getMessage();
		}
	    if ( successConnection ) {
	      MessageBox mb = new MessageBox( shell, SWT.OK | SWT.ICON_INFORMATION );
	      mb.setMessage( BaseMessages.getString( PKG, "SolrInputDialog.Connected.OK", wURL.getText())
	        + Const.CR );
	      mb.setText( BaseMessages.getString( PKG, "SolrInputDialog.Connected.Title.Ok" ) );
	      mb.open();
	    } else {
	    	if(msgError == null){
	  	      MessageBox mb = new MessageBox( shell, SWT.OK | SWT.ICON_INFORMATION );
		      mb.setMessage( BaseMessages.getString( PKG, "SolrInputDialog.Connected.StatusNOK", wURL.getText())
		        + Const.CR );
		      mb.setText( BaseMessages.getString( PKG, "SolrInputDialog.Connected.Title.Error" ) );
		      mb.open();
	    	} else {
		      new ErrorDialog(
		  	        shell,
		  	        BaseMessages.getString( PKG, "SolrInputDialog.Connected.Title.Error" ),
		  	        BaseMessages.getString( PKG, "SolrInputDialog.Connected.NOK", wURL.getText() ),
		  	        new Exception( msgError ) );
		  	    }
	    }
  }

  private void getFields() {
	  
	    try {
	      SolrInputMeta meta = new SolrInputMeta();
	      getInfo( meta );
	      // clear the current fields grid
	      wFields.removeAll();
	      // get real values
	      boolean tryCursor = true;
	      boolean facetRequested = false;
	      Integer startRecord = 0;
	      Integer chunkRowSize = 20;
	      String realURL = transMeta.environmentSubstitute( meta.getURL() );
	      String realQ = transMeta.environmentSubstitute( meta.getQ() );
	      String realSort = transMeta.environmentSubstitute( meta.getSort() );
	      String realCursor = transMeta.environmentSubstitute( meta.getCursor() );
	      String realFq = transMeta.environmentSubstitute( meta.getFq() );
	      String realFl = transMeta.environmentSubstitute( meta.getFl() );
	      String realFacetQuery = transMeta.environmentSubstitute( meta.getFacetQuery() );
	      String realFacetField = transMeta.environmentSubstitute( meta.getFacetField() );
		  /* Send and Get the report */
	      SolrQuery query = new SolrQuery();
	      query.set("rows", chunkRowSize);
	      if ( realQ != null && !realQ.equals("") ){
	    	  query.set("q", realQ);
	      }
	      if ( realSort != null && !realSort.equals("") ){
	    	  query.set("sort", realSort);
	      } else {
	    	  tryCursor = false;
	      }
	      if ( realCursor != null && !realCursor.equals("") ){
	    	  if( realCursor.equals("No") ){
	    		  tryCursor = false;
	    	  }
	      }
	      if ( realFl != null && !realFl.equals("") ){
	    	  query.set("fl", realFl);
	      }
	      if ( realFq != null && !realFq.equals("") ){
	    	  query.set("fq", realFq);
	      }
	      if ( realFacetField != null && !realFacetField.equals("")){
	    	  //TODO incorporate multiple facet fields at once
	    	  //String[] facetFields = realFacetField.split("\\s*,\\s*");
	    	  //for(int i =0; i < facetFields.length; i++){
	    	  query.addFacetField(realFacetField);  
	    	  //}
    		  query.setFacet(true);
    		  query.setFacetLimit(-1);
    		  query.setFacetMinCount(0);
    		  query.setFacetSort("count");
    		  query.set("rows", 0);
    		  tryCursor = false;
    		  facetRequested = true;
	      }
	      if ( realFacetQuery != null && !realFacetQuery.equals("")){
	    	  query.addFacetQuery(realFacetQuery);
	      }
	      // You can't use "TimeAllowed" with "CursorMark"
	      // The documentation says "Values <= 0 mean 
	      // no time restriction", so setting to 0.
	      query.setTimeAllowed(0);
	      HttpSolrServer solr = new HttpSolrServer(realURL);
	      String cursorMark = CursorMarkParams.CURSOR_MARK_START;
	      boolean done = false;
	      List<String> headerNames = new ArrayList<String>();
	      QueryResponse rsp = null;
	      while (!done) {
	    	  if(tryCursor){
    		  	query.set(CursorMarkParams.CURSOR_MARK_PARAM, cursorMark);
	    	  } else {
	    		query.setStart(startRecord);
	    	  }
	        try {
	          rsp = solr.query(query);
	        } catch (SolrServerException e) {
	          e.printStackTrace();
	        }
	        if(facetRequested){
	        	headerNames.add(rsp.getFacetFields().get(0).getName()); 
	        	headerNames.add("count"); 
	        	done = true;
	        } else {
		        SolrDocumentList docs = rsp.getResults();
		        for(SolrDocument doc : docs) {
			   	    Collection<String> thisNamesArray = doc.getFieldNames();
		    	    String[] a = thisNamesArray.toArray(new String[thisNamesArray.size()]);
				    for (int j=0; j < a.length; j++){
				         if(!headerNames.contains(a[j])){
				        	 headerNames.add(a[j]);                           
				         }
				    }
		        }
	        }
	        if(tryCursor){
		        String nextCursorMark = rsp.getNextCursorMark();
		        if (cursorMark.equals(nextCursorMark)) {
		          done = true;
		        } else {
		          cursorMark = nextCursorMark;
		        }
	        } else {
		        startRecord = startRecord + chunkRowSize;
		        if(startRecord >= rsp.getResults().getNumFound()){
		        	done = true;
		        }
	        }
	      }
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
	        shell, BaseMessages.getString( PKG, "SolrInputMeta.ErrorRetrieveData.DialogTitle" ), BaseMessages
	          .getString( PKG, "SolrInputMeta.ErrorRetrieveData.DialogMessage" ), e );
	  }
  }
 
  // Preview the data
  private void getPreview() {
    try {
    	
      SolrInputMeta oneMeta = new SolrInputMeta();
      getInfo( oneMeta );

      // check if the path is given

      TransMeta previewMeta =
        TransPreviewFactory.generatePreviewTransformation( transMeta, oneMeta, wStepname.getText() );

      EnterNumberDialog numberDialog = new EnterNumberDialog( shell, props.getDefaultPreviewSize(),
        BaseMessages.getString( PKG, "SolrInputDialog.NumberRows.DialogTitle" ),
        BaseMessages.getString( PKG, "SolrInputDialog.NumberRows.DialogMessage" ) );
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
        .getString( PKG, "SolrInputDialog.ErrorPreviewingData.DialogTitle" ), BaseMessages.getString(
        PKG, "SolrInputDialog.ErrorPreviewingData.DialogMesFacetFieldge" ), e );
    }
  }

private void getInfo( SolrInputMeta in ) {

    stepname = wStepname.getText(); // return value
    
    in.setURL( wURL.getText() );
    in.setQ( wQ.getText() );
    in.setSort( wSort.getText() );
    in.setCursor( wQuCursor.getText() );
    in.setFq( wFq.getText() );
    in.setFl( wFl.getText() );
    in.setFacetQuery( wFacetQuery.getText() );
    in.setFacetField( wFacetField.getText() );

    int nrFields = getTableView().nrNonEmpty();

    in.allocate( nrFields );

    for ( int i = 0; i < nrFields; i++ ) {
      SolrInputField field = new SolrInputField();

      TableItem item = wFields.getNonEmpty( i );

      field.setName( item.getText( 1 ) );
      field.setType( ValueMetaBase.getType( item.getText( 2 ) ) );
      field.setFormat( item.getText( 3 ) );
      field.setLength( Const.toInt( item.getText( 4 ), -1 ) );
      field.setPrecision( Const.toInt( item.getText( 5 ), -1 ) );
      field.setCurrencySymbol( item.getText( 6 ) );
      field.setDecimalSymbol( item.getText( 7 ) );
      field.setGroupSymbol( item.getText( 8 ) );
      field.setTrimType( SolrInputField.getTrimTypeByDesc( item.getText( 9 ) ) );
      
      in.getInputFields()[i] = field;
    }
  }

  /**
   * Read the data from the SolrInputMeta object and show it in this dialog.
   * @param in The SolrInputMeta object to obtain the data from.
   */
  public void getData( SolrInputMeta in ) {
	  
    wURL.setText( Const.NVL( in.getURL(), "" ) );
    wQ.setText( Const.NVL( in.getQ(), "" ) );
    wSort.setText( Const.NVL( in.getSort(), "" ) );
    wQuCursor.setText( Const.NVL( in.getCursor(), "" ) );
    wFq.setText( Const.NVL( in.getFq(), "" ) );
    wFl.setText( Const.NVL( in.getFl(), "" ) );
    wFacetField.setText( Const.NVL( in.getFacetField(), "" ) );
    wFacetQuery.setText( Const.NVL( in.getFacetQuery(), "" ) );
    if ( log.isDebug() ) {
      logDebug( BaseMessages.getString( PKG, "SolrInputDialog.Log.GettingFieldsInfo" ) );
    }
    
    for ( int i = 0; i < in.getInputFields().length; i++ ) {
    	SolrInputField field = in.getInputFields()[i];

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

  SolrInputMeta getInput() {
    return input;
  }

  void setInput( SolrInputMeta input ) {
    this.input = input;
  }
}
