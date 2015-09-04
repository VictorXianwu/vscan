import os
import sys

root = os.path.dirname(__file__)
sys.path.insert(0, os.path.join(root, 'site-packages'))

import tornado.web
import handler
import sae

settings = {
    "debug": True,
}

application = tornado.web.Application([
    (r"/", handler.MainHandler),
    (r'/qrcode/(\S+)', handler.GetQrcodeContent),
    (r'/freeqc/', handler.FreeQc),
], **settings)
