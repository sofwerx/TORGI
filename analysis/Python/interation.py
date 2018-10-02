import sqlite3
import csv
import os
import pandas as pd

con = sqlite3.connect(":memory:")
import_path = '/home/summerintern18/Torgi/SatData/Unprocessed'
export_path ='/home/Torgi/SatData/Unprocessed'
descrip =  'Control-'

x = 83

for filename in os.listdir(import_path):

    x = x + 1
    if filename.endswith(".gpkg"):
        con = sqlite3.connect(":memory:")
        sqlite_file = import_path + filename
        print(filename)
        conn = sqlite3.connect(sqlite_file)
        cur = conn.cursor()
        data =cur.execute("SELECT id,svid,constellation,cn0,agc,azimuth_deg,elevation_deg FROM sat_data;")

        export_csv = descrip + str(x) + '.csv'

        with open(export_csv, 'wb') as csvf:
            writer = csv.writer(csvf)
            writer.writerow(
                ['id', 'svid', 'constellation', 'cn0', 'agc', 'azimuth_deg', 'elevation_deg',"fileName", "CONUS", "fileID" , filename, 0 , descrip + str(x)])
            writer.writerows(data)

            for row in export_csv:
                input_file = open(export_csv, "r+")
                reader_file = csv.reader(input_file)
                value = len(list(reader_file))

                # use whatever index for the value, or however you want to construct your new value

