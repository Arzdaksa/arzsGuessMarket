package server;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

// POST /upload with the XML file as the raw request body; parsed straight off the request stream
public class UploadServlet extends ApiServlet {

    private static final long MAX_UPLOAD_BYTES = 5L * 1024 * 1024;

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        respond(response, () -> {
            if (request.getContentLengthLong() > MAX_UPLOAD_BYTES) {
                throw new IllegalArgumentException("XML file is too large.");
            }
            try (InputStream body = limited(request.getInputStream())) {
                return market().upload(username(request), body);
            }
        });
    }

    // guards against a body that is larger than its declared (or missing) Content-Length
    private static InputStream limited(InputStream in) {
        return new FilterInputStream(in) {
            private long remaining = MAX_UPLOAD_BYTES;

            @Override
            public int read() throws IOException {
                int b = super.read();
                if (b >= 0) count(1);
                return b;
            }

            @Override
            public int read(byte[] buffer, int offset, int length) throws IOException {
                int read = super.read(buffer, offset, length);
                if (read > 0) count(read);
                return read;
            }

            private void count(int bytes) throws IOException {
                remaining -= bytes;
                if (remaining < 0) throw new IOException("XML file is too large.");
            }
        };
    }
}
