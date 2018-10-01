import sqlite3
import csv
import os
import pandas as pd

con = sqlite3.connect(":memory:")
import_path = '/home/summerintern18/Torgi/GPKG-DATA/'
export_path = '/home/summerintern18/Torgi/satData'
descrip = 'Control-'

x = 0

for filename in os.listdir(import_path):

    x = x + 1
    if filename.endswith(".gpkg"):
        con = sqlite3.connect(":memory:")
        sqlite_file = import_path + filename
        export_csv = descrip + str(x) + '.csv'
        conn = sqlite3.connect(sqlite_file)
        cur = conn.cursor()

        cur.execute("INSERT INTO sat_data (conus,fileId , filename VALUES(?,?,?';)", (0, export_csv, filename))
        data1 = cur.execute("SELECT id,svid,constellation,cn0,agc,azimuth_deg,elevation_deg FROM sat_data;")

        data = {'0', filename, export_csv}
        with open(export_csv, 'wb') as csvf:
            writer = csv.writer(csvf)
            writer.writerow(
                ['id', 'svid', 'constellation', 'cn0', 'agc', 'azimuth_deg', 'elevation_deg'])
            writer.writerows(data + data1)
            for row in export_csv:
                input_file = open(export_csv, "r+")
                reader_file = csv.reader(input_file)
                value = len(list(reader_file))
                # use whatever index for the value, or however you want to construct your new value
                row = descrip + str(x)
                writer.writerow(row)
