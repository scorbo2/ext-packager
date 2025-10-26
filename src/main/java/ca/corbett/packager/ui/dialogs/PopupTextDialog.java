package ca.corbett.packager.ui.dialogs;

import ca.corbett.forms.fields.LabelField;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

/**
 * TODO remove me if swing-extras 149 gets addressed - it's basically that.
 *
 * @author <a href="https://github.com/scorbo2">scorbo2</a>
 */
public class PopupTextDialog extends JDialog {

    public static int lastWidth = 600; // arbitrary default
    public static int lastHeight = 400; // arbitrary default
    private JTextArea textArea;

    public PopupTextDialog(Dialog owner, String label, String text, boolean isEnabled) {
        super(owner, label);
        setModal(true);
        setSize(lastWidth, lastHeight);
        setLocationRelativeTo(owner);
        setResizable(true);
        initComponents(text, isEnabled);
    }

    public PopupTextDialog(Frame owner, String label, String text, boolean isEnabled) {
        super(owner, label, true);
        setSize(new Dimension(lastWidth, lastHeight));
        setLocationRelativeTo(owner);
        setResizable(true);
        initComponents(text, isEnabled);
    }

    public String getText() {
        return textArea.getText();
    }

    private void initComponents(String text, boolean isEnabled) {
        setLayout(new BorderLayout());
        add(buildTextArea(text), BorderLayout.CENTER);
        add(buildButtonPanel(), BorderLayout.SOUTH);
        textArea.setEditable(isEnabled);

        // Add ComponentListener to track resize events
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                // Update static variables with current size
                Dimension currentSize = getSize();
                lastWidth = currentSize.width;
                lastHeight = currentSize.height;
            }
        });
    }

    private JScrollPane buildTextArea(String text) {
        textArea = new JTextArea(text);
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        textArea.setFont(LabelField.getDefaultFont());
        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        return scrollPane;
    }

    private JPanel buildButtonPanel() {
        JPanel panel = new JPanel(new BorderLayout());

        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton button = new JButton("Copy");
        button.setPreferredSize(new Dimension(90, 23));
        button.addActionListener(e -> {
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(textArea.getText()), null);
        });
        leftPanel.add(button);

        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        button = new JButton("OK");
        button.setPreferredSize(new Dimension(90, 23));
        button.addActionListener(e -> {
            dispose();
        });
        rightPanel.add(button);

        panel.add(leftPanel, BorderLayout.WEST);
        panel.add(rightPanel, BorderLayout.EAST);
        return panel;
    }
}