package ca.corbett.packager.ui.dialogs;

import ca.corbett.extras.properties.PropertiesDialog;
import ca.corbett.forms.Alignment;
import ca.corbett.forms.FormPanel;
import ca.corbett.forms.fields.ComboField;
import ca.corbett.forms.fields.LabelField;
import ca.corbett.packager.ui.MainWindow;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.util.Arrays;

public class SignatureChoiceDialog extends JDialog {

    public enum Choice {
        SIGN_MISSING("Sign jars only if their signature is missing"),
        SIGN_MISSING_OR_FAILED("Sign jars only if their signature is missing or invalid"),
        SIGN_EVERYTHING("Sign all jars and overwrite any previous signatures");

        private final String label;

        Choice(String label) {
            this.label = label;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    private boolean wasOkayed;
    private final ComboField<Choice> comboField;

    public SignatureChoiceDialog() {
        super(MainWindow.getInstance(), "Sign extension jars", true);
        setSize(new Dimension(500, 200));
        setResizable(false);
        setLocationRelativeTo(MainWindow.getInstance());
        setLayout(new BorderLayout());
        FormPanel formPanel = new FormPanel(Alignment.TOP_LEFT);
        formPanel.setBorderMargin(16);
        formPanel.add(new LabelField("How would you like to sign the extension jars?"));
        comboField = new ComboField<>(null, Arrays.stream(Choice.values()).toList(), 0);
        formPanel.add(comboField);
        add(PropertiesDialog.buildScrollPane(formPanel), BorderLayout.CENTER);
        add(buildButtonPanel(), BorderLayout.SOUTH);
    }

    public Choice getChoice() {
        return comboField.getSelectedItem();
    }

    public boolean wasOkayed() {
        return wasOkayed;
    }

    private void buttonHandler(boolean okay) {
        wasOkayed = okay;
        dispose();
    }

    private JPanel buildButtonPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        panel.setBorder(BorderFactory.createRaisedBevelBorder());

        JButton button = new JButton("OK");
        button.setPreferredSize(new Dimension(90, 24));
        button.addActionListener(e -> buttonHandler(true));
        panel.add(button);

        button = new JButton("Cancel");
        button.setPreferredSize(new Dimension(90, 24));
        button.addActionListener(e -> buttonHandler(false));
        panel.add(button);

        return panel;
    }
}
