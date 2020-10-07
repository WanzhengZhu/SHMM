import time
import datetime
from dateutil.parser import parse

class Timestamp:

    def __init__(self, start_time_string='2000-01-01 00:00:00'):
        self.start_time_string = start_time_string
        pass

    def get_timestamp(self, time_string, granularity='sec'):
        # print 'start time:', self.start_time_string
        start_ts = time.mktime(parse(self.start_time_string).timetuple())  # in second
        current_ts = time.mktime(parse(time_string).timetuple())  # in second
        timestamp = current_ts - start_ts
        if granularity == 'sec':
            return int(timestamp)
        elif granularity == 'min':
            return int(timestamp / 60)
        elif granularity == 'hour':
            return int(timestamp / 3600)
        else:
            print 'The granularity can be sec, min, or hour!'
            return 0

    '''
    Get the weekday and hour for an input timestamp
    Weekday is a number from 1 to 7
    '''
    def get_day_hour(self, timestamp, granularity='sec'):
        struct_time = self._to_struct_time(timestamp, granularity)
        weekday = struct_time.isoweekday()
        hour = struct_time.hour
        return weekday, hour

    def _to_struct_time(self, timestamp, granularity='sec'):
        start_ts = time.mktime(parse(self.start_time_string).timetuple())
        # the default input granularity is in second
        if granularity == 'sec':
            delta = timestamp
        elif granularity == 'hour':
            delta *= 60
        elif granularity == 'hour':
            delta *= 3600
        else:
            print 'The input timestamp can be sec, min, or hour!'
        current_timestamp = start_ts + delta
        return datetime.datetime.fromtimestamp(current_timestamp)

if __name__ == '__main__':
    print 'hello'
    # t = Timestamp('%Y-%m-%d %X', '2013-02-08 00:00:00')
    t = Timestamp('2013-02-08 00:00:00')
    ts = t.get_timestamp('2013-02-08 04:00:00', 'sec')
    print ts, t.get_day_hour(ts, 'sec')
