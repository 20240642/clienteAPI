package clienteapi;
/**Clase: EarthquakeApp.java
 *@Description clase que consulta la API de terremotos de USGS 
 *              y permite ver los resultados en una tabla.
 * @author José Juan Cruz Horta
 * @version 1.2
 * @Date 2024-10-30
 */

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class EarthquakeApp {
    private JFrame frame;
    private JTextField startDateTextField;
    private JTextField endDateTextField;
    private JTextField minMagnitudeTextField;
    private JTable table;
    private DefaultTableModel tableModel;

    public EarthquakeApp() {
        frame = new JFrame("Earthquake Information");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(900, 600);
        frame.setLayout(new BorderLayout());

        // Panel superior con campos de fecha y magnitud mínima
        JPanel panel = new JPanel();
        startDateTextField = new JTextField(10);
        endDateTextField = new JTextField(10);
        minMagnitudeTextField = new JTextField(5);
        JButton fetchButton = new JButton("Fetch Earthquakes");

        panel.add(new JLabel("Start Date (YYYY-MM-DD):"));
        panel.add(startDateTextField);
        panel.add(new JLabel("End Date (YYYY-MM-DD):"));
        panel.add(endDateTextField);
        panel.add(new JLabel("Min Magnitude:"));
        panel.add(minMagnitudeTextField);
        panel.add(fetchButton);
        frame.add(panel, BorderLayout.NORTH);

        // Tabla para mostrar los datos de los terremotos
        tableModel = new DefaultTableModel(new String[]{"Place", "Time", "Magnitude", "Depth (km)"}, 0);
        table = new JTable(tableModel);
        frame.add(new JScrollPane(table), BorderLayout.CENTER);

        // Evento para el botón de búsqueda
        fetchButton.addActionListener(e -> fetchEarthquakes());

        frame.setVisible(true);
    }

    private void fetchEarthquakes() {
        String startDate = startDateTextField.getText();
        String endDate = endDateTextField.getText();
        String minMagnitudeText = minMagnitudeTextField.getText();
        double minMagnitude = Double.parseDouble(minMagnitudeText.isEmpty() ? "0" : minMagnitudeText);

        try {
            String apiUrl = String.format(
                "https://earthquake.usgs.gov/fdsnws/event/1/query?format=geojson&starttime=%s&endtime=%s&minmagnitude=%.1f",
                startDate, endDate, minMagnitude
            );
            URL url = new URL(apiUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.connect();

            int responseCode = connection.getResponseCode();
            if (responseCode != 200) {
                JOptionPane.showMessageDialog(null, "Error: " + responseCode + " " + connection.getResponseMessage());
                return;
            }

            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();

            JSONObject jsonResponse = new JSONObject(response.toString());
            JSONArray featuresArray = jsonResponse.getJSONArray("features");

            tableModel.setRowCount(0);

            for (int i = 0; i < featuresArray.length(); i++) {
                JSONObject feature = featuresArray.getJSONObject(i);
                JSONObject properties = feature.getJSONObject("properties");

                double magnitude = properties.getDouble("mag");
                if (magnitude >= minMagnitude) {
                    String place = properties.getString("place");
                    long time = properties.getLong("time");
                    double depth = feature.getJSONObject("geometry").getJSONArray("coordinates").getDouble(2);

                    // Formatear el tiempo de Unix a una fecha legible
                    String timeStr = LocalDate.ofEpochDay(time / (1000 * 60 * 60 * 24))
                                             .format(DateTimeFormatter.ISO_DATE);

                    tableModel.addRow(new Object[]{place, timeStr, magnitude, depth});
                }
            }

            if (tableModel.getRowCount() == 0) {
                JOptionPane.showMessageDialog(null, "No earthquakes found in the specified range.");
            }

        } catch (IOException | JSONException e) {
            JOptionPane.showMessageDialog(null, "Error fetching earthquake data: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(EarthquakeApp::new);
    }
}

