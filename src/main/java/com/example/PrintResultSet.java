package com.example;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;

import com.datastax.oss.driver.api.core.cql.ColumnDefinition;
import com.datastax.oss.driver.api.core.cql.ColumnDefinitions;
import com.datastax.oss.driver.api.core.cql.ResultSet;
import com.datastax.oss.driver.api.core.cql.Row;
import com.datastax.oss.driver.api.core.data.UdtValue;
import com.datastax.oss.driver.api.core.type.DataType;
import com.datastax.oss.driver.api.core.type.ListType;
import com.datastax.oss.driver.api.core.type.MapType;
import com.datastax.oss.driver.api.core.type.SetType;
import com.datastax.oss.driver.api.core.type.UserDefinedType;

/**
 * Classe utilitária para formatar ResultsSets
 */
@Service
public class PrintResultSet {

    public void printTableFormat(ResultSet resultSet) {
        ColumnDefinitions columnDefinitions = resultSet.getColumnDefinitions();
        List<List<String>> rows = new ArrayList<>();

        // Obter os nomes das colunas
        List<String> columnNames = new ArrayList<>();
        for (ColumnDefinition columnDefinition : columnDefinitions) {
            String columnName = columnDefinition.getName().asInternal();
            columnNames.add(columnName);
        }

        // Adicionar os nomes das colunas como primeira linha
        rows.add(columnNames);

        // Adicionar os valores das colunas para cada linha
        int rowCount = 0;
        for (Row row : resultSet) {
            rowCount++;
            List<String> values = new ArrayList<>();
            for (ColumnDefinition columnDefinition : columnDefinitions) {
                var dataType = columnDefinition.getType();
                String columnName = columnDefinition.getName().asInternal();

                if (dataType instanceof UserDefinedType) {
                    String columnValue = row.get(columnName, UdtValue.class).toString();
                    values.add(columnValue);
                } else if (dataType instanceof ListType) {
                    List<?> columnValue = row.getList(columnName, Object.class);
                    values.add(columnValue.toString());
                } else if (dataType instanceof SetType) {
                    Set<?> columnValue = row.getSet(columnName, Object.class);
                    values.add(columnValue.toString());
                } else if (dataType instanceof MapType) {
                    Map<?, ?> columnValue = row.getMap(columnName, Object.class, Object.class);
                    values.add(columnValue.toString());
                } else {
                    Object columnValue = row.getObject(columnName);
                    values.add(String.valueOf(columnValue));
                }
            }
            rows.add(values);
        }

        // Imprimir a tabela
        printTable(rows);

        // Imprimir a quantidade de linhas
        System.out.println("\nQuantidade de linhas: " + rowCount);
        System.out.println("");
    }

    private void printTable(List<List<String>> rows) {
        // Determinar a largura de cada coluna
        int[] columnWidths = new int[rows.get(0).size()];
        for (List<String> row : rows) {
            for (int i = 0; i < row.size(); i++) {
                columnWidths[i] = Math.max(columnWidths[i], row.get(i).length());
            }
        }

        // Imprimir as linhas formatadas
        for (int rowIndex = 0; rowIndex < rows.size(); rowIndex++) {
            List<String> row = rows.get(rowIndex);

            // Imprimir linha de separação após o nome das colunas
            if (rowIndex == 1) {
                for (int i = 0; i < row.size(); i++) {
                    String line = "-".repeat(columnWidths[i] + 2); // Adicionar 2 para espaço extra
                    System.out.print(line);
                }
                System.out.println();
            }

            for (int i = 0; i < row.size(); i++) {
                String format = "%-" + (columnWidths[i] + 2) + "s"; // Adicionar 2 para espaço extra
                System.out.printf(format, row.get(i));
            }
            System.out.println();
        }
    }

    public void printFormFormat(ResultSet resultSet) {
        System.out.println("==========================");
        int rowCount = 0;
        for (Row row : resultSet) {
            rowCount++;
            for (ColumnDefinition columnDefinition : resultSet.getColumnDefinitions()) {
                String columnName = columnDefinition.getName().asInternal();
                DataType dataType = columnDefinition.getType();

                Object columnValue;
                if (dataType instanceof UserDefinedType) {
                    columnValue = row.get(columnName, UdtValue.class).toString();
                } else if (dataType instanceof ListType) {
                    columnValue = row.getList(columnName, Object.class);
                } else if (dataType instanceof SetType) {
                    columnValue = row.getSet(columnName, Object.class);
                } else if (dataType instanceof MapType) {
                    columnValue = row.getMap(columnName, Object.class, Object.class);
                } else {
                    columnValue = row.getObject(columnName);
                }

                System.out.println(columnName + ": " + columnValue);
            }
            System.out.println("--------------------------");
        }
        // Imprimir a quantidade de linhas
        System.out.println("\nQuantidade de linhas: " + rowCount);
        System.out.println("");
    }

}
