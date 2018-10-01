

import sqlite3
import csv

con = sqlite3.connect(":memory:")

sqlite_file = '/home/summerintern18/Torgi/GPKG-DATA/TORGI-GNSS-20180826T122015.527Z.gpkg'
conn = sqlite3.connect(sqlite_file)
cur = conn.cursor()
data = cur.execute("SELECT id,svid,constellation,cn0,agc,azimuth_deg,elevation_deg FROM sat_data;")
with open('satData/output.csv', 'wb') as f:
    writer = csv.writer(f)
    writer.writerow(['id', 'svid', 'constellation', 'cn0', 'agc', 'azimuth_deg', 'elevation_deg',"conus", "orig_name", "id_name"])
    writer.writerows(data)

print(cur.fetchall())





