/*
 * Syncany, www.syncany.org
 * Copyright (C) 2011-2013 Philipp C. Heckel <philipp.heckel@gmail.com> 
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.syncany.gui.wizard;

import java.util.LinkedHashMap;
import java.util.Map;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.Text;
import org.syncany.crypto.CipherSpec;
import org.syncany.crypto.CipherSpecs;
import org.syncany.gui.CommonParameters;
import org.syncany.gui.UserInput;
import org.syncany.gui.WidgetDecorator;
import org.syncany.util.I18n;
import org.syncany.util.StringUtil;
import org.syncany.util.StringUtil.StringJoinListener;

/**
 * @author Vincent Wiencek <vwiencek@gmail.com>
 *
 */
public class RepositoryEncryptionPanel extends WizardPanelComposite {
	private Text passwordText;
	private Text passwordAgainText;
	private Combo cypherCombo;
	private Spinner chunkSize;
	private Button enableEncryption;
	private Label chunkSizeLabel; 
	private Label algorithmLabel;
	private Composite stackComposite;
	private Composite encryptionComposite;
	private Composite emptyComposite;
	private StackLayout stackLayout = new StackLayout();
	
	@SuppressWarnings("serial")
	private Map<String, CipherSpec[]> cipherOptions = new LinkedHashMap<String, CipherSpec[]>(){{
		put("AES (128)", new CipherSpec[]{CipherSpecs.getCipherSpec(CipherSpecs.AES_128_GCM)});
		put("AES (256)", new CipherSpec[]{CipherSpecs.getCipherSpec(CipherSpecs.AES_256_GCM)});
		put("TwoFish (128)", new CipherSpec[]{CipherSpecs.getCipherSpec(CipherSpecs.TWOFISH_128_GCM)});
		put("TwoFish (256)", new CipherSpec[]{CipherSpecs.getCipherSpec(CipherSpecs.TWOFISH_256_GCM)});
		put("AES & TwoFish (128)", new CipherSpec[]{CipherSpecs.getCipherSpec(CipherSpecs.AES_128_GCM), CipherSpecs.getCipherSpec(CipherSpecs.TWOFISH_128_GCM)});
		put("AES & TwoFish (256)", new CipherSpec[]{CipherSpecs.getCipherSpec(CipherSpecs.AES_256_GCM), CipherSpecs.getCipherSpec(CipherSpecs.TWOFISH_256_GCM)});
	}};
	
	public RepositoryEncryptionPanel(WizardDialog wizardParentDialog, Composite parent, int style) {
		super(wizardParentDialog, parent, style);
		initComposite();
		
		toggleEncryptionSelection();
	}
	
	public void initComposite(){
		GridLayout gridLayout = new GridLayout(2, false);
		gridLayout.marginRight = 30;
		setLayout(gridLayout);
		
		Label introductionTextTitle = new Label(this, SWT.WRAP);
		introductionTextTitle.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false, 2, 1));
		introductionTextTitle.setText(I18n.getString("repository.encryption.introduction.title"));
		
		Label introductionText = new Label(this, SWT.WRAP);
		introductionText.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false, 2, 1));
		introductionText.setText(I18n.getString("repository.encryption.introduction"));
		
		Label passwordLabel = new Label(this, SWT.NONE);
		passwordLabel.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
		passwordLabel.setText(I18n.getString("repository.encryption.password", true));
		
		passwordText = new Text(this, SWT.BORDER | SWT.PASSWORD);
		passwordText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		
		Label passwordLabelAgain = new Label(this, SWT.NONE);
		passwordLabelAgain.setText(I18n.getString("repository.encryption.passwordAgain", true));
		
		passwordAgainText = new Text(this, SWT.BORDER | SWT.PASSWORD);
		passwordAgainText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		
		stackComposite = new Composite(this, SWT.NONE);
		stackComposite.setLayout(stackLayout);
		GridData gd_composite = new GridData(SWT.FILL, SWT.FILL, false, false, 2, 1);
		gd_composite.verticalIndent = 23;
		stackComposite.setLayoutData(gd_composite);
		
		encryptionComposite = new Composite(stackComposite, SWT.NONE);
		encryptionComposite.setLayout(new GridLayout(2, false));
		
		chunkSizeLabel = new Label(encryptionComposite, SWT.NONE);
		chunkSizeLabel.setSize(169, 15);
		chunkSizeLabel.setText(I18n.getString("repository.encryption.chunkSize", true));
		
		
		chunkSize = new Spinner(encryptionComposite, SWT.BORDER);
		chunkSize.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		chunkSize.setSize(208, 22);
		chunkSize.setMaximum(10000);
		chunkSize.setSelection(512);
		
		enableEncryption = new Button(encryptionComposite, SWT.CHECK);
		enableEncryption.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 2, 1));
		enableEncryption.setSize(172, 16);
		enableEncryption.setText(I18n.getString("repository.encryption.enable"));
		enableEncryption.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				toggleEncryptionSelection();
			}
		});
		
		enableEncryption.setSelection(true);
		
		algorithmLabel = new Label(encryptionComposite, SWT.NONE);
		algorithmLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		algorithmLabel.setSize(168, 15);
		algorithmLabel.setText(I18n.getString("repository.encryption.algorithm", true));
				
		cypherCombo = new Combo(encryptionComposite, SWT.NONE | SWT.READ_ONLY);
		cypherCombo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
		cypherCombo.setSize(208, 23);
		for (String key : cipherOptions.keySet()){
			CipherSpec[] cipherSpec = cipherOptions.get(key);
			String cipherSuitesIdStr = StringUtil.join(cipherSpec, ",", new StringJoinListener<CipherSpec>() {
				@Override
				public String getString(CipherSpec cipherSpec) {
					return ""+cipherSpec.getId();
				}			
			});
			cypherCombo.add(key);
			cypherCombo.setData(key, cipherSuitesIdStr);
		}
		cypherCombo.select(4);
						
		emptyComposite = new Composite(stackComposite, SWT.NONE);
		
		WidgetDecorator.bold(introductionTextTitle);
		WidgetDecorator.normal(introductionText, passwordAgainText, passwordText);
	}
	
	protected void toggleEncryptionSelection() {
		cypherCombo.setEnabled(enableEncryption.getSelection());
		algorithmLabel.setForeground(enableEncryption.getSelection() ? WidgetDecorator.BLACK : WidgetDecorator.GRAY);
	}

	public boolean issValid() {
		return 
			passwordText.getText().length() > 6 && 
			passwordText.getText().equals(passwordAgainText.getText()); 
	}
	
	@Override
	public boolean isValid() {
		String password = passwordText.getText();
		String passwordAgain = passwordAgainText.getText();
		
		WidgetDecorator.markAs(password != null && password.length() > 6, passwordText);
		WidgetDecorator.markAs(password != null && password.length() > 6 && password.equals(passwordAgain), passwordAgainText);
		
		return password != null && password.length() > 6 && password.equals(passwordAgain);
	}

	@Override
	public UserInput getUserSelection() {
		UserInput userInput = new UserInput();
		userInput.putCommonParameter(CommonParameters.ENCRYPTION_PASSWORD, passwordText.getText());
		userInput.putCommonParameter(CommonParameters.CHUNK_SIZE, chunkSize.getText());
		
		if (enableEncryption.getSelection()){
			userInput.putCommonParameter(CommonParameters.ENCRYPTION_ENABLED, enableEncryption.getSelection() ? "yes" : "no");
			
			String cipherSpecsString = cypherCombo.getItem(cypherCombo.getSelectionIndex());
			String cipherSpecsId = (String)cypherCombo.getData(cipherSpecsString);
			userInput.putCommonParameter(CommonParameters.ENCRYPTION_ALGORITHM, cipherSpecsId);
		}
		return userInput;
	}

	@Override
	public boolean hasNextButton() {
		return true;
	}

	@Override
	public boolean hasPreviousButton() {
		return true;
	}

	@Override
	public boolean hasFinishButton() {
		return false;
	}

	@Override
	public void updateData() {
		String action = getParentWizardDialog().getUserInput().getCommonParameter(CommonParameters.COMMAND_ACTION);
		
		if (action.equals("create")) {
			showPanel(this.encryptionComposite);
		}
		else {
			showPanel(this.emptyComposite);
		}
	}
	
	private void showPanel(Composite composite) {
		stackLayout.topControl = composite;
		stackComposite.layout();
	}
}
