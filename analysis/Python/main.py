import sqlite3
import csv
import os
import pandas as pd


def individual_output():
    con = sqlite3.connect(":memory:")
    import_path = '/home/summerintern18/Torgi/GPKG-DATA/'  ###Import Directory Path
    descrip = 'Control-'  ### FileId Category Identifier
    print(import_path)
    x = 83    ####FileId starting number

    for filename in os.listdir(import_path):
        try:
            x = x + 1
            fileId = descrip + str(x)

            if filename.endswith(".gpkg"):
                con = sqlite3.connect(":memory:")
                print(filename)
                sqlite_file = import_path + filename
                conn = sqlite3.connect(sqlite_file)
                cur = conn.cursor()
                data = cur.execute("SELECT id,svid,constellation,cn0,agc,azimuth_deg,elevation_deg FROM sat_Data;")  ### SQL selection statement

                export_csv = descrip + str(x) + '.csv'  ###fileId CSV Name

                with open(export_csv, 'wb') as csvf:
                    writer = csv.writer(csvf)
                    writer.writerow(
                        ['id', 'svid', 'constellation', 'cn0', 'agc', 'azimuth_deg', 'elevation_deg',"fileName", "CONUS", "fileId" ])     ### Creats header row for CSV file
                    writer.writerows(data)
                    conn.close()

            output_df = pd.read_csv(export_csv)
            ### Fills in Columns with Static data  to Columns added that were provided via the SQL statement
            output_df['CONUS'] = '0'  ### Working to add automatic detection for future use
            output_df['fileID'] = fileId
            output_df['fileName'] = filename

            output_df.to_csv(export_csv)
            print(export_csv)

        except sqlite3.Error:
            print(filename + " is empty")


def all_output(): ### Creates the AllOutput csv
    allOutput = "allOutput.csv"
    with open(allOutput, 'wb') as csvfile:
        fileWriter = csv.writer(csvfile)
        fileWriter.writerow(
            ['id', 'svid', 'constellation', 'cn0', 'agc', 'azimuth_deg', 'elevation_deg', "fileName", "CONUS",
             "fileId"])
        print(allOutput)


def combine():
    csv_path = '/home/summerintern18/Torgi/Python/'
    for f in os.listdir(csv_path):

        if f.endswith(".csv"):
            try:
                combined_csv = pd.concat([pd.read_csv(f)])

                combined_csv.to_csv("combined_csv.csv", index=False)

            except IOError:
                print(IOError)


def main():
    individual_output()
    all_output()
    combine()


main()

