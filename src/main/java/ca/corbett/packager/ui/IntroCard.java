package ca.corbett.packager.ui;

import ca.corbett.extras.LookAndFeelManager;
import ca.corbett.extras.properties.LookAndFeelProperty;
import ca.corbett.forms.Alignment;
import ca.corbett.forms.FormPanel;
import ca.corbett.forms.Margins;
import ca.corbett.forms.fields.ComboField;
import ca.corbett.forms.fields.LabelField;
import ca.corbett.packager.AppConfig;
import ca.corbett.packager.Version;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;

public class IntroCard extends JPanel {

    public IntroCard() {
        setLayout(new BorderLayout());
        FormPanel formPanel = new FormPanel(Alignment.TOP_LEFT);
        formPanel.setBorderMargin(new Margins(12));
        formPanel.add(LabelField.createBoldHeaderLabel("Overview", 20));

        formPanel.add(
                LabelField.createPlainHeaderLabel("Automate the creation of your application/extension update json!"));

        formPanel.add(LabelField.createPlainHeaderLabel("Refer to the project page for full documentation:"));
        LabelField label = new LabelField(Version.PROJECT_URL);
        label.setHyperlink(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                MainWindow.getInstance().openHyperlink(Version.PROJECT_URL);
            }
        });
        label.getMargins().setLeft(48);
        formPanel.add(label);

        label = LabelField.createPlainHeaderLabel("The short version:");
        label.getMargins().setTop(12);
        formPanel.add(label);

        label = new LabelField("1. Select/create a project");
        label.getMargins().setTop(0);
        label.getMargins().setBottom(0);
        label.getMargins().setLeft(48);
        formPanel.add(label);

        label = new LabelField("2. Select/create a keypair");
        label.getMargins().setTop(0);
        label.getMargins().setBottom(0);
        label.getMargins().setLeft(48);
        formPanel.add(label);

        label = new LabelField("3. Specify your web host");
        label.getMargins().setTop(0);
        label.getMargins().setBottom(0);
        label.getMargins().setLeft(48);
        formPanel.add(label);

        label = new LabelField("4. Describe your application/extensions");
        label.getMargins().setTop(0);
        label.getMargins().setBottom(0);
        label.getMargins().setLeft(48);
        formPanel.add(label);

        label = new LabelField("5. Sign and upload!");
        label.getMargins().setTop(0);
        label.getMargins().setLeft(48);
        formPanel.add(label);

        JPanel footerPanel = new JPanel(new BorderLayout());
        footerPanel.setBorder(BorderFactory.createRaisedBevelBorder());
        FormPanel footerForm = new FormPanel(Alignment.CENTER);
        footerForm.setBorderMargin(0);

        LookAndFeelProperty lafProperty = AppConfig.getInstance().getLookAndFeelProp();
        //noinspection unchecked
        final ComboField<String> lafCombo = (ComboField<String>)lafProperty.generateFormField();
        lafCombo.addValueChangedListener(field -> {
            LookAndFeelManager.switchLaf(lafProperty.getLafClass(lafCombo.getSelectedIndex()));
            lafProperty.loadFromFormField(lafCombo);
            AppConfig.getInstance().save();
        });
        lafCombo.getMargins().setAll(2);
        footerForm.add(lafCombo);
        footerPanel.add(footerForm, BorderLayout.CENTER);

        add(formPanel, BorderLayout.CENTER);
        add(footerPanel, BorderLayout.SOUTH);
    }
}
