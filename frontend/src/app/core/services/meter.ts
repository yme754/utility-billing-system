import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';
import { AuthService } from './auth';

@Injectable({
  providedIn: 'root'
})
export class MeterService {
  private apiUrl = 'http://localhost:8080/readings';

  constructor(private http: HttpClient, private authService: AuthService) {}
  private getHeaders() {
    const token = this.authService.getToken();
    return new HttpHeaders()
      .set('Authorization', `Bearer ${token}`)
      .set('Content-Type', 'application/json');
  }
  submitReading(readingPayload: any): Observable<any> {
    return this.http.post(this.apiUrl, readingPayload, { headers: this.getHeaders() });
  }
  getReadingsForCurrentMonth(): Observable<any[]> {
    return this.http.get<any[]>(`${this.apiUrl}/current-month`, { headers: this.getHeaders() });
  }
}