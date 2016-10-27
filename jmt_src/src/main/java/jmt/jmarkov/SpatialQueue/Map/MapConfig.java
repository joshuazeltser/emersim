package jmt.jmarkov.SpatialQueue.Map;

import com.teamdev.jxmaps.*;
import com.teamdev.jxmaps.swing.MapView;

import javax.swing.*;
import javax.swing.plaf.basic.BasicButtonUI;
import javax.swing.plaf.basic.BasicTextFieldUI;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class MapConfig extends MapView {

    private static final String INITIAL_LOCATION = "Imperial College London, SW7 2AZ";
    private OptionsWindow optionsWindow;
    private boolean placeMarker;

    public MapConfig(MapViewOptions options) {
        super(options);
        setOnMapReadyHandler(new MapReadyHandler() {
            @Override
            public void onMapReady(MapStatus status) {
                // Getting the associated map object
                final Map map = getMap();
                // Setting initial zoom value
                map.setZoom(7.0);
                // Creating a map options object
                MapOptions options = new MapOptions(map);
                // Creating a map type control options object
                MapTypeControlOptions controlOptions = new MapTypeControlOptions(map);
                // Changing position of the map type control
                controlOptions.setPosition(ControlPosition.TOP_RIGHT);
                // Setting map type control options
                options.setMapTypeControlOptions(controlOptions);
                // Setting map options
                map.setOptions(options);
                performGeocode(INITIAL_LOCATION);
                map.addEventListener("click", new MapMouseEvent() {
                    @Override
                    public void onEvent(MouseEvent mouseEvent) {
                        if (placeMarker) {
                            // Closing initially created info window
                            infoWindow.close();
                            // Creating a new marker
                            final Marker marker = new Marker(map);
                            // Move marker to the position where user clicked
                            marker.setPosition(mouseEvent.latLng());
                            placeMarker = false;
                        }
                    }
                });
            }
        });
    }

    @Override
    public void addNotify() {
        super.addNotify();

        optionsWindow = new OptionsWindow(this, new Dimension(350, 40)) {
            @Override
            public void initContent(JWindow contentWindow) {
                JPanel content = new JPanel(new GridBagLayout());
                content.setBackground(Color.white);

                Font robotoPlain13 = new Font("Roboto", 0, 13);
                final JTextField searchField = new JTextField();
                searchField.setText(INITIAL_LOCATION);
                searchField.setToolTipText("Enter address or coordinates...");
                searchField.setBorder(BorderFactory.createEmptyBorder());
                searchField.setFont(robotoPlain13);
                searchField.setForeground(new Color(0x21, 0x21, 0x21));
                searchField.setUI(new SearchFieldUI(searchField));

                final JButton searchButton = new JButton();
                searchButton.setIcon(new ImageIcon(this.getClass().getResource("res/search.png")));
                searchButton.setRolloverIcon(new ImageIcon(this.getClass().getResource("res/search_hover.png")));
                searchButton.setBorder(BorderFactory.createEmptyBorder());
                searchButton.setUI(new BasicButtonUI());
                searchButton.setOpaque(false);
                ActionListener searchActionListener = new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        performGeocode(searchField.getText());
                    }
                };
                searchButton.addActionListener(searchActionListener);
                searchField.addActionListener(searchActionListener);

                content.add(searchField, new GridBagConstraints(0, 0, 1, 1, 1.0, 0.0,
                        GridBagConstraints.NORTHWEST, GridBagConstraints.HORIZONTAL, new Insets(11, 11, 11, 0), 0, 0));
                content.add(searchButton, new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0,
                        GridBagConstraints.NORTHWEST, GridBagConstraints.NONE, new Insets(11, 0, 11, 11), 0, 0));

                contentWindow.getContentPane().add(content);
            }

            @Override
            protected void updatePosition() {
                if (parentFrame.isVisible()) {
                    java.awt.Point newLocation = parentFrame.getContentPane().getLocationOnScreen();
                    newLocation.translate(250, 11);
                    contentWindow.setLocation(newLocation);
                    contentWindow.setSize(340, 40);
                }
            }
        };
    }

    class SearchFieldUI extends BasicTextFieldUI {
        private final JTextField textField;

        public SearchFieldUI(JTextField textField) {
            this.textField = textField;
        }

        @Override
        protected void paintBackground(Graphics g) {
            super.paintBackground(g);
            String toolTipText = textField.getToolTipText();
            String text = textField.getText();
            if (toolTipText != null && text.isEmpty()) {
                paintPlaceholderText(g, textField);
            }
        }

        protected void paintPlaceholderText(Graphics g, JComponent c) {
            g.setColor(new Color(0x75, 0x75, 0x75));
            g.setFont(c.getFont());
            String text = textField.getToolTipText();
            if (g instanceof Graphics2D) {
                Graphics2D graphics2D = (Graphics2D) g;
                graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            }
            g.drawString(text, 0, 14);
        }
    }

    @Override
    public void removeNotify() {
        super.removeNotify();
        optionsWindow.dispose();
    }

    Marker currentLocation;
    InfoWindow infoWindow;

    private void performGeocode(String text) {
        // Getting the associated map object
        final Map map = getMap();
        // Creating a geocode request
        GeocoderRequest request = new GeocoderRequest(map);
        // Setting address to the geocode request
        request.setAddress(text);

        // Geocoding position by the entered address
        getServices().getGeocoder().geocode(request, new GeocoderCallback(map) {
            @Override
            public void onComplete(GeocoderResult[] results, GeocoderStatus status) {
                // Checking operation status
                if ((status == GeocoderStatus.OK) && (results.length > 0)) {
                    // Getting the first result
                    GeocoderResult result = results[0];
                    // Getting a location of the result
                    LatLng location = result.getGeometry().getLocation();
                    // Setting the map center to result location
                    map.setCenter(location);
                    // Creating a marker object
                    if (currentLocation == null)
                        currentLocation = new Marker(map);
                    // Setting position of the marker to the result location
                    currentLocation.setPosition(location);
                    // Creating an information window
                    if (infoWindow == null)
                        infoWindow = new InfoWindow(map);
                    // Putting the address and location to the content of the information window
                    infoWindow.setContent("<b>" + result.getFormattedAddress() + "</b><br>" + location.toString());
                    // Moving the information window to the result location
                    infoWindow.setPosition(location);
                    // Showing of the information window
                    infoWindow.open(map, currentLocation);
                }
            }
        });
    }

    public void toggleMarkerPlacement() {
        placeMarker = true;
    }
}