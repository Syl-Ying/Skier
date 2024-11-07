package org.sylvia;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.Year;

@WebServlet(value = "/skiers/*")
public class SkierServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        resp.setContentType("text/plain");
        String pathInfo = req.getPathInfo();

        // validate url path and return response code and maybe some value if input is valid
        if (!isUrlValid(pathInfo, resp)) {
            return;
        } else {
            resp.setStatus(HttpServletResponse.SC_OK); // 200
            // TODO: process url params in `urlParts`
            resp.getWriter().write("200 It Works");
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String pathInfo = req.getPathInfo(); // /{resortID}/seasons/{seasonID}/days/{dayID}/skiers/{skierID}:
        if (!isUrlValid(pathInfo, resp)) {
            return;
        }

        // TODO: parse JSon with req.getReader() which returns a BufferedReader
        //  convert to a String for further JSON processing.
        String[] pathParts = pathInfo.split("/");
        int resortID = Integer.parseInt(pathParts[1]);
        String seasonID = pathParts[3];
        int dayID = Integer.parseInt(pathParts[5]);
        int skierID = Integer.parseInt(pathParts[7]);

        // Todo: parse the request body and handle the lift ride data
        resp.setStatus(HttpServletResponse.SC_CREATED); // 201
        writeResponse(resp, String.format("Lift ride stored for skierID %d at resort %d on day %d in %s",
                skierID, resortID, dayID, seasonID));
    }

    private Boolean isUrlValid(String urlPath, HttpServletResponse resp) throws IOException {
        // check if the url exists
        if (urlPath == null || urlPath.isEmpty()) {
            resp.setStatus(HttpServletResponse.SC_NOT_FOUND);  // 404
            resp.getWriter().write("missing parameters");
            return false;
        }

        String[] pathParts = urlPath.split("/");

        // validate the request url path according to the API spec
        // urlPath  = "/1/seasons/2019/day/1/skier/123"
        // pathParts = [, 1, seasons, 2019, day, 1, skier, 123]
        if (pathParts.length != 8) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            writeResponse(resp, "Invalid path, expected format: /{resortID}/seasons/{seasonID}/days/{dayID}/skiers/{skierID}");
            return false;
        }

        try {
            // Validate resortID
            int resortID = Integer.parseInt(pathParts[1]);
            if (resortID <= 0) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                writeResponse(resp, "Invalid resortID. Must be a positive integer.");
                return false;
            }

            // Validate seasonID
            String seasonID = pathParts[3];
            int seasonYear = Integer.parseInt(seasonID);
            int currentYear = Year.now().getValue();
            if (seasonYear < 1900 || seasonYear > currentYear) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                writeResponse(resp, "Invalid seasonID. Must be a valid year between 1900 and " + currentYear + " .");
                return false;
            }

            // Validate dayID range (1-366)
            int dayID = Integer.parseInt(pathParts[5]);
            if (dayID < 1 || dayID > 366) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                writeResponse(resp, "Invalid dayID, must be between 1 and 366.");
                return false;
            }

            // Validate skierID
            int skierID = Integer.parseInt(pathParts[7]);
            if (skierID <= 0) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                writeResponse(resp, "Invalid skierID. Must be a positive integer.");
                return false;
            }
        } catch (NumberFormatException e) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            writeResponse(resp, "Invalid parameter format. resortID, dayID, and skierID must be integers.");
            return false;
        }

        return true;
    }

    private void writeResponse(HttpServletResponse resp, String message) throws IOException {
        resp.setContentType("application/json");
        PrintWriter out = resp.getWriter();
        out.print("{ \"message\": \"" + message + "\" }");
        out.flush();
    }
}
