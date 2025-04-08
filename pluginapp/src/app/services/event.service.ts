import { Injectable, isDevMode } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from 'src/environments/environment.development';

interface UniqueEvents {
  Events: string[];
}

export interface EventDetail {
  targetName: string;
  createdDate: string;
  endDate: string;
  uniqueHash: string;
  taskResultId: string;
  targetDisplayName: string;
  targetId: string;
  requesterDisplayName: string;
  requesterId: string;
  identityRequestId: string;
  type: string;
  identityRequestCompletionStatusString: string;
  taskResultName: string;
  taskResultStatus: string;
}

interface Events {
  [key: string]: EventDetail[];
}

declare const PluginHelper: {
  getCsrfToken: Function
  getPluginRestUrl: Function
  getCurrentUsername: Function
};

declare const SailPoint: {
  CONTEXT_PATH: string
};

@Injectable({
  providedIn: 'root'
})

export class EventService {
  private headers!: HttpHeaders;
  private iiqUrl: string;
  private pluginUrl: any;
  private token: any;
  private username: string;

  constructor(private http: HttpClient) { 
    if (isDevMode()) {
      console.log("DEV mode");
      this.iiqUrl = '/identityiq'; // use proxy
      this.pluginUrl = '/plugin/rest/EventsDashboardPlugin';
      this.username = environment.username;
      let encoded = btoa(environment.username + ':' + environment.password);
      this.headers = new HttpHeaders({
        "Authorization": "Basic " + encoded,
        "Content-Type": "application/json",
        'Referrer-Policy': 'no-referrer'
      })
    } else {
      console.log("Prod mode?");
      this.pluginUrl = PluginHelper.getPluginRestUrl('EventsDashboardPlugin');
      this.username = PluginHelper.getCurrentUsername();
      this.iiqUrl = "";
      this.token = PluginHelper.getCsrfToken();
      this.headers = new HttpHeaders({
        "X-XSRF-TOKEN": this.token,
        "Content-Type": "application/json"
      })
    }
  }

  getUniqueEvents(): Observable<UniqueEvents> {
    return this.http.get<UniqueEvents>(this.iiqUrl + this.pluginUrl + '/getUniqueEvents', { headers: this.headers });
  }

  getAllEvents(eventName?: string, limit?: number): Observable<Events> {
    let params: { [key: string]: string } = {};
    if (eventName) params['event'] = eventName;
    if (limit) params['limit'] = limit.toString();

    return this.http.get<Events>(this.iiqUrl + this.pluginUrl + '/getAllEvents', { 
      headers: this.headers,
      params: params
    });
  }

  getEventWithFilter(eventName: string, pageLimit: number, pageNumber: number, startDate: string, endDate: string): Observable<Events> {
    let params: { [key: string]: string } = {
      event: eventName,
      limit: pageLimit.toString(),
      page: pageNumber.toString(),
      startDate: startDate,
      endDate: endDate
    };
    return this.http.get<Events>(this.iiqUrl + this.pluginUrl + '/getEventWithFilter', { 
      headers: this.headers,
      params: params
    });
  }

  convertToCSV(data: EventDetail[]): string {
    const headers = ['Identity Request ID', 'Identity Name', 'Identity Display Name', 'Requester', 'Identity Request Status', 'Task Result Name', 'Task Result Status', 'Created Date', 'Completed Date'];
    const rows = data.map(item => [
      item.identityRequestId,
      item.targetName,
      item.targetDisplayName,
      item.requesterDisplayName,
      item.identityRequestCompletionStatusString,
      item.taskResultName,
      item.taskResultStatus,
      item.createdDate,
      item.endDate
    ]);
    return [headers.join(','), ...rows.map(row => row.join(','))].join('\n');
  }

  downloadCSV(csvContent: string, fileName: string): void {
    const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' });
    const link = document.createElement('a');
    if (link.download !== undefined) {
      const url = URL.createObjectURL(blob);
      link.setAttribute('href', url);
      link.setAttribute('download', fileName);
      link.style.visibility = 'hidden';
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
    }
  }
}