/*
 * evaluator.h
 *
 * Copyright (C) 2015 Pixelgaffer
 *
 * This work is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by the
 * Free Software Foundation; either version 2 of the License, or any later
 * version.
 *
 * This work is distributed in the hope that it will be useful, but without
 * any warranty; without even the implied warranty of merchantability or
 * fitness for a particular purpose. See version 2 and version 3 of the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

#ifndef EVALUATOR_H
#define EVALUATOR_H

#include <QFile>
#include <QList>
#include <QNetworkAccessManager>
#include <QPair>
#include <QSettings>
#include <QString>
#include <QUrl>

class BuildInstructions;
class LangSpec;

class Evaluator
{
	Q_DISABLE_COPY(Evaluator)
	
public:
	Evaluator(const BuildInstructions &instructions);
	~Evaluator();
	
	BuildInstructions instructions () const { return _instructions; }
	
	bool createLangSpecs(bool verbose = false);
	int target(const QString &target);
	
	void setHost (const QString &val) { host = val; }
	
private:
	int target(const QString &target, LangSpec *spec);
	
	QPair<QString, QByteArray> apiGetCall(const QString &destination)
	{
		return apiGetCall(QUrl((https ? "https://" : "http://") + host + destination));
	}
	QPair<QString, QByteArray> apiGetCall(const QUrl &url);
	
	QPair<QString, QByteArray> apiPostCall(const QString &destination, const QString &contentType, const QByteArray &content)
	{
		return apiPostCall(QUrl((https ? "https://" : "http://") + host + destination), contentType, content);
	}
	QPair<QString, QByteArray> apiPostCall(const QUrl &url, const QString &contentType, const QByteArray &content);
	
	QPair<QString, QByteArray> apiPostCall(const QString &destination, const QString &contentType, QFile *file)
	{
		return apiPostCall(QUrl((https ? "https://" : "http://") + host + destination), contentType, file);
	}
	QPair<QString, QByteArray> apiPostCall(const QUrl &url, const QString &contentType, QFile *file);
	
	QString createZip(const QDir &dir, QString filename = QString());
	
	BuildInstructions _instructions;
	QList<LangSpec*> langSpecs;
	
	// zeugs für upload
	QString host;
	QString email;
	QString pass;
	QNetworkAccessManager *mgr = 0;
	bool https = false;
	
	// zeugs zum cachen des hosts und der email
	QSettings pwCache;
	
};

#endif // EVALUATOR_H
