#!/usr/bin/env python
# -*- coding:utf-8 -*-
#
#   Author  :   Victor Lin
#   E-mail  :   linxianwusx@gmail.com
#   Date    :   15/01/31 18:25:06
#
import tornado.web
from tornado.httpclient import AsyncHTTPClient

class MainHandler(tornado.web.RequestHandler):
    @tornado.web.asynchronous
    def get(self):
        self.write("This is linxianwu's website")
        self.finish()

    def _callback(self, response):
        self.write(response.body)
        self.finish()
