package com.mishagrodno.faceRecognizer.application;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Represents application main form.
 *
 * @author Gomanchuk Mikhail.
 */
public class MainForm {

    private JFrame mainFrame;
    private JPanel mainPanel;
    private JPanel panel;
    private JLabel image;
    private JTextField textName;
    private JLabel labelName;
    private JButton saveButton;
    private JButton reloadButton;

    private boolean toSave;

    public void create(final int height, final int width) {

        mainFrame = new JFrame("Face Recognizing");
        mainFrame.setSize(width, height);

        mainPanel = new JPanel(new GridBagLayout());

        final GridBagConstraints constraints = new GridBagConstraints();
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.insets = new Insets(10, 10, 10, 10);

        image = new JLabel();
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.gridwidth = 4;
        constraints.gridheight = 4;
        mainPanel.add(image, constraints);

        panel = new JPanel(new GridBagLayout());

        labelName = new JLabel("Name");
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.gridwidth = 1;
        constraints.gridheight = 1;
        labelName.setFont(new Font("Arial", Font.PLAIN, 16));
        panel.add(labelName, constraints);

        textName = new JTextField(20);
        constraints.gridx = 1;
        constraints.gridy = 0;
        textName.setFont(new Font("Arial", Font.PLAIN, 16));
        panel.add(textName, constraints);

        saveButton = new JButton("save");
        saveButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                toSave = true;
            }
        });

        constraints.gridx = 1;
        constraints.gridy = 2;
        saveButton.setFont(new Font("Arial", Font.PLAIN, 16));
        panel.add(saveButton, constraints);

        reloadButton = new JButton("reload");
        constraints.gridx = 1;
        constraints.gridy = 3;
        reloadButton.setFont(new Font("Arial", Font.PLAIN, 16));
        panel.add(reloadButton, constraints);

        mainFrame.add(mainPanel);
        mainPanel.add(panel);

        mainFrame.pack();
        mainFrame.setVisible(true);
    }

    public JFrame getMainFrame() {
        return mainFrame;
    }

    public JPanel getMainPanel() {
        return mainPanel;
    }

    public JPanel getPanel() {
        return panel;
    }

    public JLabel getImage() {
        return image;
    }

    public JTextField getTextName() {
        return textName;
    }

    public JLabel getLabelName() {
        return labelName;
    }

    public JButton getSaveButton() {
        return saveButton;
    }

    public boolean isToSave() {
        return toSave;
    }

    public void setToSave(boolean toSave) {
        this.toSave = toSave;
    }

    public JButton getReloadButton() {
        return reloadButton;
    }

    public void setReloadButton(JButton reloadButton) {
        this.reloadButton = reloadButton;
    }
}
