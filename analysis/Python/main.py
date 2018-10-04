import sqlite3
import csv
import os
import pandas as pd

con = sqlite3.connect(":memory:")
import_path = '/home/summerintern18/Torgi/SatData/Unprocessed/'
descrip = 'Control-'
print(import_path)
x = 83


def individual_output():
for filename in os.listdir(import_path):

    x = x + 1
    fileId = descrip + str(x)
    if filename.endswith(".gpkg"):
        con = sqlite3.connect(":memory:")

        sqlite_file = import_path + filename
        conn = sqlite3.connect(sqlite_file)
        cur = conn.cursor()
        data = cur.execute("SELECT id,svid,constellation,cn0,agc,azimuth_deg,elevation_deg FROM Sat_Data;")

        export_csv = descrip + str(x) + '.csv'

        with open(export_csv, 'wb') as csvf:
            writer = csv.writer(csvf)
            writer.writerow(
                ['id', 'svid', 'constellation', 'cn0', 'agc', 'azimuth_deg', 'elevation_deg',"fileName", "CONUS", "fileId" ])
            writer.writerows(data)
            conn.close()
    engine ='python'
    output_df = pd.read_csv(export_csv, export_csv)
    engine = 'python'
    output_df = pd.read_csv(export_csv, export_csv)
    output_df['CONUS'] = '0'
    output_df['fileID'] = fileId
    output_df['fileName'] = filename
    count = output_df.count()
    output_df.to_csv(export_csv)
