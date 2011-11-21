package gui.templates;

import gui.PipelineGenerator;
import gui.PipelineWindow;
import gui.widgets.FileSelectionListener;
import gui.widgets.FileSelectionPanel;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JTextArea;

public class AnnotateVariants extends TemplateConfigurator {

	public AnnotateVariants(PipelineWindow window) {
		super(window);

		generator = new PipelineGenerator( PipelineWindow.getFileResource("templates/annotate_variants.xml"));
		initComponents();
	}

	protected void initComponents() {
		//JPanel centerPanel = new JPanel();
		setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		
		JTextArea descArea = new JTextArea("Description: " + generator.getDescription().replace('\n', ' '));
		descArea.setWrapStyleWord(true);
		descArea.setEditable(false);
		descArea.setOpaque(false);
		descArea.setBorder(BorderFactory.createLineBorder(Color.gray, 1, true));
		descArea.setAlignmentX(Component.RIGHT_ALIGNMENT);
		descArea.setLineWrap(true);
		descArea.setMaximumSize(new Dimension(1000, 100));
		descArea.setPreferredSize(new Dimension(400, 100));
		add(descArea, BorderLayout.NORTH );
		
		add(Box.createVerticalStrut(20));
		add(Box.createVerticalGlue());
		refBox = new JComboBox(refTypes);
		JPanel refPanel = new JPanel();
		refPanel.setLayout(new FlowLayout(FlowLayout.RIGHT));
		refPanel.add(new JLabel("<html><b>Reference:</b></html>"));
		refPanel.add(refBox);
		refPanel.setAlignmentX(Component.RIGHT_ALIGNMENT);
		add(refPanel);
		
		chooser = new JFileChooser( System.getProperty("user.home"));
		readsOnePanel = new FileSelectionPanel("Input variants (vcf) file:", "Choose file", chooser);
		readsOnePanel.addListener(new FileSelectionListener() {
			public void fileSelected(File file) {
				if (file != null) {
					generator.inject("inputVCF", file.getAbsolutePath());
				}
			}
		});
		
		
		
		readsOnePanel.setAlignmentX(Component.RIGHT_ALIGNMENT);
		add(readsOnePanel);
		add(Box.createVerticalGlue());
		
		
		JPanel bottomPanel = new JPanel();
		bottomPanel.setLayout(new BorderLayout());
		add(new JSeparator(JSeparator.HORIZONTAL));
		
		JButton beginButton = new JButton("Begin");
		beginButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				updateReference();
				window.beginRun(generator.getDocument());
			}
		});
		beginButton.setAlignmentX(Component.RIGHT_ALIGNMENT);
		add(beginButton);
	}
	
}
