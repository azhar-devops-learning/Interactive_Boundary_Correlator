package com.custom.jmeter;

import org.apache.jmeter.processor.gui.AbstractPreProcessorGui;
import org.apache.jmeter.testelement.TestElement;
import org.apache.jmeter.gui.GuiPackage;
import org.apache.jmeter.gui.tree.JMeterTreeNode;
import org.apache.jmeter.protocol.http.sampler.HTTPSamplerBase;
import org.apache.jmeter.config.Argument;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.BorderFactory;
import javax.swing.JOptionPane;

public class BoundaryUUIDCorrelatorGui extends AbstractPreProcessorGui {

    private static final String LEFT_BOUNDARY = "EntityID%22%3A%22";
    private static final String RIGHT_BOUNDARY = "%22%2C%22IsDirty";
    private static final String REGEX = LEFT_BOUNDARY + "(.*?)" + RIGHT_BOUNDARY;
    private static final Pattern PATTERN = Pattern.compile(REGEX);
    private static final String VAR_REF = "${DYNAMIC_UUID}";

    private JTextField foundUuidField;
    private JLabel statusLabel;

    public BoundaryUUIDCorrelatorGui() {
        super();
        init();
    }

    @Override
    public String getLabelResource() {
        return this.getClass().getSimpleName();
    }

    @Override
    public String getStaticLabel() {
        return "Interactive Boundary Correlator";
    }

    @Override
    public TestElement createTestElement() {
        BoundaryUUIDCorrelator preProcessor = new BoundaryUUIDCorrelator();
        modifyTestElement(preProcessor);
        return preProcessor;
    }

    @Override
    public void modifyTestElement(TestElement element) {
        super.configureTestElement(element);
    }

    private void init() {
        setLayout(new BorderLayout(0, 10));
        setBorder(makeBorder());
        add(makeTitlePanel(), BorderLayout.NORTH);

        JPanel mainPanel = new JPanel(new BorderLayout(5, 5));
        
        // Status and Information Panel
        JPanel infoPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        infoPanel.add(new JLabel("Found UUID:"));
        foundUuidField = new JTextField(30);
        foundUuidField.setEditable(false);
        infoPanel.add(foundUuidField);
        statusLabel = new JLabel("Status: Ready");
        infoPanel.add(statusLabel);
        mainPanel.add(infoPanel, BorderLayout.NORTH);

        // Buttons Panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton btnFind = new JButton("Find UUID");
        JButton btnCorrelate = new JButton("Correlate");
        JButton btnRevert = new JButton("Revert");
        JButton btnClear = new JButton("Clear");

        buttonPanel.add(btnFind);
        buttonPanel.add(btnCorrelate);
        buttonPanel.add(btnRevert);
        buttonPanel.add(btnClear);
        mainPanel.add(buttonPanel, BorderLayout.CENTER);

        add(mainPanel, BorderLayout.CENTER);

        // --- BUTTON ACTION LISTENERS ---

        btnFind.addActionListener(e -> {
            String body = getParentBodyData();
            if (body != null) {
                Matcher matcher = PATTERN.matcher(body);
                if (matcher.find()) {
                    foundUuidField.setText(matcher.group(1));
                    statusLabel.setText("Status: UUID Found!");
                } else {
                    statusLabel.setText("Status: UUID not found in parent HTTP Request.");
                }
            }
        });

        btnCorrelate.addActionListener(e -> {
            String oldUuid = foundUuidField.getText();
            if (!oldUuid.isEmpty()) {
                String body = getParentBodyData();
                if (body != null && body.contains(oldUuid)) {
                    String updatedBody = body.replace(oldUuid, VAR_REF);
                    setParentBodyData(updatedBody);
                    statusLabel.setText("Status: Correlated successfully! Replaced with " + VAR_REF);
                }
            } else {
                JOptionPane.showMessageDialog(this, "Please 'Find UUID' first.");
            }
        });

        btnRevert.addActionListener(e -> {
            String oldUuid = foundUuidField.getText();
            if (!oldUuid.isEmpty()) {
                String body = getParentBodyData();
                if (body != null && body.contains(VAR_REF)) {
                    String revertedBody = body.replace(VAR_REF, oldUuid);
                    setParentBodyData(revertedBody);
                    statusLabel.setText("Status: Reverted successfully.");
                }
            } else {
                JOptionPane.showMessageDialog(this, "No UUID history to revert to. Find UUID first.");
            }
        });

        btnClear.addActionListener(e -> {
            foundUuidField.setText("");
            statusLabel.setText("Status: Cleared.");
        });
    }

    // --- HELPER METHODS TO ACCESS PARENT HTTP REQUEST ---

    private HTTPSamplerBase getParentSampler() {
        JMeterTreeNode currentNode = GuiPackage.getInstance().getTreeListener().getCurrentNode();
        if (currentNode != null && currentNode.getParent() != null) {
            JMeterTreeNode parentNode = (JMeterTreeNode) currentNode.getParent();
            TestElement parentElement = parentNode.getTestElement();
            if (parentElement instanceof HTTPSamplerBase) {
                return (HTTPSamplerBase) parentElement;
            }
        }
        return null;
    }

    private String getParentBodyData() {
        HTTPSamplerBase sampler = getParentSampler();
        if (sampler != null && sampler.getArguments().getArgumentCount() > 0) {
            Argument arg = sampler.getArguments().getArgument(0);
            return arg.getValue();
        }
        return null;
    }

    private void setParentBodyData(String newBody) {
        HTTPSamplerBase sampler = getParentSampler();
        if (sampler != null && sampler.getArguments().getArgumentCount() > 0) {
            Argument arg = sampler.getArguments().getArgument(0);
            arg.setValue(newBody);
            // Optional: Tell JMeter that the parent node has been modified so it prompts for saving
            GuiPackage.getInstance().updateCurrentGui(); 
        }
    }
}