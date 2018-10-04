import sqlite3
import csv
import os
import pandas as pd


def individual_output():
    con = sqlite3.connect(":memory:")
    import_path = '/home/summerintern18/Torgi/GPKG-DATA/'
    descrip = 'Control-'
    print(import_path)
    x = 83
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
                data = cur.execute("SELECT id,svid,constellation,cn0,agc,azimuth_deg,elevation_deg FROM sat_Data;")

                export_csv = descrip + str(x) + '.csv'

                with open(export_csv, 'wb') as csvf:
                    writer = csv.writer(csvf)
                    writer.writerow(
                        ['id', 'svid', 'constellation', 'cn0', 'agc', 'azimuth_deg', 'elevation_deg',"fileName", "CONUS", "fileId" ])
                    writer.writerows(data)
                    conn.close()

            output_df = pd.read_csv(export_csv, export_csv)
            output_df['CONUS'] = '0'
            output_df['fileID'] = fileId
            output_df['fileName'] = filename

            output_df.to_csv(export_csv)

        except sqlite3.Error:
            print(filename + " is empty")


def main():
    individual_output()


main()
