import ast, urllib, time

# out.write('https://maps.googleapis.com/maps/api/staticmap?zoom=13&size=600x600&maptype=roadmap&')
prefix ='https://maps.googleapis.com/maps/api/staticmap?zoom=11&size=600x600&maptype=roadmap&'
cnt=0
s = set()
# event_file = '/shared/data/czhang82/event/la_tweets/visualize_true_event_locations.txt'
# event_file = '/Users/wanzheng/Desktop/UIUC-CS512-SP17/data/visualize_true_event_locations.txt'
# event_file = '/Users/wanzheng/Desktop/UIUC-CS512-SP17/data/tf-la/input/Results_LA_geo.txt'
event_file = '/Users/wanzheng/Desktop/UIUC-CS512-SP17/data/tf-ny/input/Results_NY_geo.txt'


line_num = 0
with open(event_file) as f:
    for line in f:
        line_num += 1
        if line_num != 5:
            continue
        cur_line = line.rstrip().split(':')
        items = ast.literal_eval(cur_line[1].strip())
        link = prefix
        for i in items:
            # str = 'markers=icon:https://maps.gstatic.com/intl/en_us/mapfiles/markers2/measle.png%7Ccolor:blue%7C' + '%f,%f&' % (i[0], i[1])
            str = 'markers=color:red%7C' + '%s,%s&' % (i[0], i[1])
            link += str
            # out.write(str)
            cnt+=1
            s.add(i)
            #out.write(cur_line[0]+'\t'+ str(i)+ '\n')
        urllib.urlretrieve(link, 'output/' + cur_line[0]+'.png')
        if cnt%9==0:
            time.sleep(3)
        if cnt%100==0:
            time.sleep(10)
print cnt, len(s)

# with open(event_file) as f:
#     for line in f:
#         cur_line = line.rstrip().split(':')
#         items = ast.literal_eval(cur_line[1].strip())
#         link = prefix
#         for i in items:
#             # str = 'markers=icon:https://maps.gstatic.com/intl/en_us/mapfiles/markers2/measle.png%7Ccolor:blue%7C' + '%f,%f&' % (i[0], i[1])
#             str = 'markers=color:red%7C' + '%f,%f&' % (i[0], i[1])
#             link += str
#             # out.write(str)
#             cnt+=1
#             s.add(i)
#             #out.write(cur_line[0]+'\t'+ str(i)+ '\n')
#         urllib.urlretrieve(link, 'output/' + cur_line[0]+'.png')
#         if cnt%9==0:
#             time.sleep(3)
#         if cnt%100==0:
#             time.sleep(10)
# print cnt, len(s)
