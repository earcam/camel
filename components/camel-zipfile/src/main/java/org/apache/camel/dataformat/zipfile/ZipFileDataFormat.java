/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.camel.dataformat.zipfile;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.apache.camel.Exchange;
import org.apache.camel.spi.DataFormat;
import org.apache.camel.util.IOHelper;
import org.apache.camel.util.StringHelper;

import static org.apache.camel.Exchange.FILE_NAME;

/**
 * Zip file data format.
 * See {@link org.apache.camel.model.dataformat.ZipDataFormat} for "deflate" compression.
 */
public class ZipFileDataFormat implements DataFormat {

    @Override
    public void marshal(Exchange exchange, Object graph, OutputStream stream) throws Exception {
        String filename = exchange.getIn().getHeader(FILE_NAME, String.class);
        if (filename != null) {
            filename = new File(filename).getName(); // remove any path elements
        } else {
            // generate the file name as the camel file component would do
            filename = StringHelper.sanitize(exchange.getIn().getMessageId());
        }

        ZipOutputStream zos = new ZipOutputStream(stream);
        zos.putNextEntry(new ZipEntry(filename));

        InputStream is = exchange.getContext().getTypeConverter().mandatoryConvertTo(InputStream.class, graph);

        try {
            IOHelper.copy(is, zos);
        } finally {
            IOHelper.close(is, zos);
        }

        String newFilename = filename + ".zip";
        exchange.getOut().setHeader(FILE_NAME, newFilename);
    }

    @Override
    public Object unmarshal(Exchange exchange, InputStream stream) throws Exception {
        InputStream is = exchange.getIn().getMandatoryBody(InputStream.class);
        ZipInputStream zis = new ZipInputStream(is);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        try {
            ZipEntry entry = zis.getNextEntry();
            if (entry != null) {
                exchange.getOut().setHeader(FILE_NAME, entry.getName());
                IOHelper.copy(zis, baos);
            }

            entry = zis.getNextEntry();
            if (entry != null) {
                throw new IllegalStateException("Zip file has more than 1 entry.");
            }

            return baos.toByteArray();

        } finally {
            IOHelper.close(zis, baos);
        }
    }

}
