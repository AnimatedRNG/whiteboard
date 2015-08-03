// http://www.vogella.com/tutorials/EclipseDialogs/article.html#tutorial_passworddialog

package com.animated.rng.whiteboard;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

public class PasswordDialog extends Dialog {
  private Text txtUser;
  private Text txtPassword;
  private boolean checkbox;
  private String user = "";
  private String password = "";

  public PasswordDialog(Shell parentShell) {
    super(parentShell);
    this.checkbox = false;
  }

  @Override
  protected Control createDialogArea(Composite parent) {
    Composite container = (Composite) super.createDialogArea(parent);
    GridLayout layout = new GridLayout(2, false);
    layout.marginRight = 5;
    layout.marginLeft = 10;
    container.setLayout(layout);

    Label lblUser = new Label(container, SWT.NONE);
    lblUser.setText("User:");

    txtUser = new Text(container, SWT.BORDER);
    txtUser.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false,
        1, 1));
    txtUser.setText(user);
    txtUser.addModifyListener(new ModifyListener() {

      @Override
      public void modifyText(ModifyEvent e) {
        Text textWidget = (Text) e.getSource();
        String userText = textWidget.getText();
        user = userText;
      }
    });

    Label lblPassword = new Label(container, SWT.NONE);
    GridData gd_lblNewLabel = new GridData(SWT.LEFT, SWT.CENTER, false,
        false, 1, 1);
    gd_lblNewLabel.horizontalIndent = 1;
    lblPassword.setLayoutData(gd_lblNewLabel);
    lblPassword.setText("Password:");

    txtPassword = new Text(container, SWT.BORDER| SWT.PASSWORD);
    txtPassword.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true,
        false, 1, 1));
    txtPassword.setText(password);
    txtPassword.addModifyListener(new ModifyListener() {

      @Override
      public void modifyText(ModifyEvent e) {
        Text textWidget = (Text) e.getSource();
        String passwordText = textWidget.getText();
        password = passwordText;
      }
    });
    
    Button isAdmin = new Button(container, SWT.CHECK);
    isAdmin.setText("Admin?");
    isAdmin.addSelectionListener(new SelectionAdapter() {
    	
    	@Override
    	public void widgetSelected(SelectionEvent event) {
    		 Button checkBox = (Button) event.getSource();
    		 if (checkBox.getSelection())
    			 PasswordDialog.this.checkbox = true;
    		 else
    			 PasswordDialog.this.checkbox = false;
    	}
    });
    
    return container;
  }

  // override method to use "Login" as label for the OK button
  @Override
  protected void createButtonsForButtonBar(Composite parent) {
    createButton(parent, IDialogConstants.OK_ID, "Login", true);
    createButton(parent, IDialogConstants.CANCEL_ID,
        IDialogConstants.CANCEL_LABEL, false);
  }

  @Override
  protected Point getInitialSize() {
    return new Point(500, 300);
  }

  @Override
  protected void okPressed() {
    user = txtUser.getText();
    password = txtPassword.getText();
    super.okPressed();
  }

  public String getUser() {
    return user;
  }

  public void setUser(String user) {
    this.user = user;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  public boolean isCheckbox() {
    return checkbox;
  }

} 