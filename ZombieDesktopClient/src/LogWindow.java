import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

public class LogWindow extends JFrame implements ActionListener {
	private final JTextArea text_area = new JTextArea(30, 30);
    JScrollPane scroll_pane = new JScrollPane(text_area,
    		JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
    		JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
	JButton clear_button = new JButton("Clear log"); 
	private final ClientWindow owner;

	public LogWindow(ClientWindow owner) {
		super("Zombie Client/Server Communication Log");
		this.owner = owner;
		Container pane = getContentPane();
		pane.setLayout(new BorderLayout());

		JPanel button_panel = new JPanel();
		button_panel.add(clear_button);
	    pane.add(button_panel, BorderLayout.NORTH);
	    
	    clear_button.addActionListener(this);  
	    pane.add(scroll_pane, BorderLayout.CENTER);

	    pack();
        setVisible(true);
	}

	public void from_server(String line) {
		text_area.append("S: " + line + "\n");
	}

	public void from_client(String line) {
		text_area.append("C: " + line + "\n");
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == clear_button) {
			text_area.setText("");
		}
	}
} // class LogWindow
