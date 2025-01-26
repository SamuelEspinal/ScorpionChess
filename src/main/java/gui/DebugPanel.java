package gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import java.util.ArrayList;
import java.util.List;

// Define my custom Observable interface
interface MyObservable {
    void addObserver(MyObserver observer);
    void notifyObservers(Object obj);
}

// Define my custom Observer interface
interface MyObserver {
    void update(Object obj);
}

public class DebugPanel extends JPanel implements MyObserver {
    
    private static final Dimension CHAT_PANEL_DIMENSION = new Dimension(600, 150);
    private final JTextArea jTextArea;

    // Constructor
    public DebugPanel() {
        super(new BorderLayout());
        this.jTextArea = new JTextArea("");
        add(this.jTextArea);
        setPreferredSize(CHAT_PANEL_DIMENSION);
        validate();
        setVisible(true);
    }

    public void redo() {
        validate();
    }

    @Override
    public void update(final Object obj) {
        this.jTextArea.setText(obj.toString().trim());
        redo();
    }
}

// Custom observable class
class MyObservableImpl implements MyObservable {

    private final List<MyObserver> observers = new ArrayList<>();
    private Object state;

    @Override
    public void addObserver(MyObserver observer) {
        observers.add(observer);
    }

    @Override
    public void notifyObservers(Object obj) {
        for (MyObserver observer : observers) {
            observer.update(obj);
        }
    }

    public void setState(Object newState) {
        this.state = newState;
        notifyObservers(state);
    }
}
