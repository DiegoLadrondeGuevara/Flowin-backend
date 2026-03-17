package org.example.flowin2.web.controller;

import org.apache.tomcat.util.http.fileupload.util.LimitedInputStream;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.MediaTypeFactory;
import org.apache.commons.io.input.BoundedInputStream;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.Path;
import java.nio.file.Paths;

@RestController
@RequestMapping("/music")
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class MusicController {

    private final Path musicDirectory = Paths.get("src/main/resources/static/music"); // Cambia según tu estructura

    @GetMapping("/{filename:.+}")
    public ResponseEntity<Resource> getSong(@PathVariable String filename, @RequestHeader(value = "Range", required = false) String rangeHeader) {
        try {
            Path file = musicDirectory.resolve(filename).normalize();
            UrlResource resource = new UrlResource(file.toUri());

            if (!resource.exists()) {
                return ResponseEntity.notFound().build();
            }

            long fileLength = resource.contentLength();
            long start = 0;
            long end = fileLength - 1;

            if (rangeHeader != null && rangeHeader.startsWith("bytes=")) {
                String[] ranges = rangeHeader.substring(6).split("-");
                try {
                    start = Long.parseLong(ranges[0]);
                    if (ranges.length > 1 && !ranges[1].isEmpty()) {
                        end = Long.parseLong(ranges[1]);
                    }
                } catch (NumberFormatException e) {
                    return ResponseEntity.status(HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE).build();
                }
            }

            if (start > end || start >= fileLength) {
                return ResponseEntity.status(HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE).build();
            }

            long contentLength = end - start + 1;
            HttpHeaders headers = new HttpHeaders();
            headers.set(HttpHeaders.CONTENT_TYPE, "audio/mpeg");
            headers.set(HttpHeaders.ACCEPT_RANGES, "bytes");
            headers.set(HttpHeaders.CONTENT_LENGTH, String.valueOf(contentLength));
            headers.set(HttpHeaders.CONTENT_RANGE, String.format("bytes %d-%d/%d", start, end, fileLength));
            headers.set(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"");

            long finalStart = start;

            InputStreamResource inputStream = new InputStreamResource(resource.getInputStream()) {
                @Override
                public InputStream getInputStream() throws IOException {
                    InputStream fullStream = super.getInputStream();
                    fullStream.skip(finalStart);
                    return new BoundedInputStream(fullStream, contentLength);
                }
            };

            return new ResponseEntity<>(inputStream, headers, rangeHeader != null ? HttpStatus.PARTIAL_CONTENT : HttpStatus.OK);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

}
