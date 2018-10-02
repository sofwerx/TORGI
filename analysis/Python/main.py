import sqlite3
import csv
import os
import pandas as pd

con = sqlite3.connect(":memory:")
import_path = 'your_Directory'
descrip = 'Control-'   ### Change this to

print(import_path)

x = 83

for filename in os.listdir(import_path):

    x = x + 1

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
                ['id', 'svid', 'constellation', 'cn0', 'agc', 'azimuth_deg', 'elevation_deg',"fileName", "CONUS", "fileID" , filename, 0 , descrip + str(x)])
            writer.writerows(data)
            conn.close()




