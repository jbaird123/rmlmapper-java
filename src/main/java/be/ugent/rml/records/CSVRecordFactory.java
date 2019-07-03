package be.ugent.rml.records;

import be.ugent.rml.NAMESPACES;
import be.ugent.rml.Utils;
import be.ugent.rml.access.Access;
import be.ugent.rml.store.QuadStore;
import be.ugent.rml.term.Literal;
import be.ugent.rml.term.NamedNode;
import be.ugent.rml.term.Term;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class CSVRecordFactory implements ReferenceFormulationRecordFactory {

    @Override
    public List<Record> getRecords(Access access, Term logicalSource, QuadStore rmlStore) throws IOException {
        List<Term> sources = Utils.getObjectsFromQuads(rmlStore.getQuads(logicalSource, new NamedNode(NAMESPACES.RML + "source"), null));
        Term source = sources.get(0);
        CSVParser parser;

        if (source instanceof Literal) {
            parser = getParserForNormalCSV(access);
        } else {
            List<Term> sourceType = Utils.getObjectsFromQuads(rmlStore.getQuads(source, new NamedNode(NAMESPACES.RDF + "type"), null));

            if (sourceType.get(0).getValue().equals(NAMESPACES.CSVW + "Table")) {
                CSVW csvw = new CSVW(access.getInputStream(), rmlStore, logicalSource);
                parser = csvw.getCSVParser();
            } else {
                parser = getParserForNormalCSV(access);
            }
        }

        if (parser != null) {

            List<org.apache.commons.csv.CSVRecord> myEntries = parser.getRecords();

            return myEntries.stream()
                    .map(record -> new CSVRecord(record, access.getDataTypes()))
                    .collect(Collectors.toList());
        } else {
            return new ArrayList<>();
        }
    }


    private CSVParser getParserForNormalCSV(Access access) throws IOException {
        CSVFormat csvFormat = CSVFormat.DEFAULT.withHeader().withSkipHeaderRecord(false);
        InputStream inputStream = access.getInputStream();

        if (inputStream != null) {
            try {
                return CSVParser.parse(inputStream, StandardCharsets.UTF_8, csvFormat);
            } catch (IllegalArgumentException e) {
                // TODO give warning to user
                return null;
            }
        } else {
            return null;
        }
    }
}
